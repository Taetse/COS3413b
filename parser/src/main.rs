use std::{env, process, fmt::Debug, fs::File};
use std::io::{self, Read, Write};

mod parser;
mod pruner;

use parser::{Node, Token};

struct Args {
    input_file: String,
    concrete_file: String,
    abstract_file: String,
}

fn parse_args() -> Option<Args> {
    let mut args = env::args().skip(1);
    Some(Args {
        input_file: args.next()?,
        concrete_file: args.next()?,
        abstract_file: args.next()?,
    })
}

fn read_input<'a>(filename: &str, input: &'a mut String) -> Result<Vec<Token<'a>>, String> {
    if filename == "-" {
        read_tokens(&mut io::stdin(), input)
    } else {
        read_tokens(
            &mut File::open(filename).map_err(|e| format!("{}", e))?,
            input,
        )
    }
}

fn read_tokens<'a, R: Read>(src: &mut R, input: &'a mut String) -> Result<Vec<Token<'a>>, String> {
    use Token::*;
    let mut tokens = Vec::new();
    src.read_to_string(input).map_err(|e| format!("{}", e))?;
    for line in input.lines() {
        let mut words = line.splitn(4, ' ').skip(2);
        let class = words.next().ok_or("Missing token class")?;
        let snippet = words.next().ok_or("Missing token snippet")?;
        tokens.push(match class {
            "Arithmetic" => Arithmetic(snippet),
            "Assignment" => Assignment(snippet),
            "Boolean" => Boolean(snippet),
            "Comparison" => Comparison(snippet),
            "Control" => Control(snippet),
            "Grouping" => Grouping(snippet),
            "IO" => IO(snippet),
            "Name" => Name(snippet),
            "Number" => Number(snippet),
            "Procedure" => Procedure(snippet),
            "Special" => Special(snippet),
            "String" => String(snippet),
            "Truth" => Truth(snippet),
            "Type" => Type(snippet),
            s => return Err(format!("Invalid token class '{}'", s)),
        });
    }
    Ok(tokens)
}

fn write_output<N: Debug>(filename: &str, tree: &Node<N>) -> io::Result<()> {
    let mut index = 1;
    let mut indent = String::new();
    if filename == "-" {
        write_tree(&mut io::stdout(), tree, 0, &mut index, &mut indent)?;
    } else {
        write_tree(
            &mut File::create(&filename)?,
            tree,
            0,
            &mut index,
            &mut indent,
        )?;
    }
    Ok(())
}

fn write_tree<W: Write, N: Debug>(
    dest: &mut W,
    tree: &Node<N>,
    index: usize,
    next_index: &mut usize,
    indent: &mut String,
) -> io::Result<()> {
    write!(dest, "{}{} {:?}", indent, index, tree.info)?;
    let base = *next_index;
    for i in 0..tree.children.len() {
        write!(dest, " {}", base + i)?;
        *next_index += 1;
    }
    writeln!(dest, "")?;
    indent.push(' ');
    for (i, child) in tree.children.iter().enumerate() {
        write_tree(dest, child, base + i, next_index, indent)?;
    }
    indent.pop();
    Ok(())
}

fn run_parser() -> Result<(), String> {
    let args = parse_args().ok_or("Usage: parser <input_file> <concrete_file> <abstract_file>")?;
    let mut input = String::new();
    let tokens = read_input(&args.input_file, &mut input)
        .map_err(|e| format!("Error reading '{}': {}.", args.input_file, e))?;
    let concrete_tree = parser::parse(tokens).map_err(|e| {
        format!(
            "Syntax Error: Unexpected token {:?} at index {}.",
            e.token, e.index
        )
    })?;
    write_output(&args.concrete_file, &concrete_tree)
        .map_err(|e| format!("Error writing to '{}': {}.", args.concrete_file, e))?;
    let abstract_tree = pruner::prune(&concrete_tree);
    write_output(&args.abstract_file, &abstract_tree)
        .map_err(|e| format!("Error writing to '{}': {}.", args.abstract_file, e))?;
    Ok(())
}

fn main() {
    if let Err(msg) = run_parser() {
        eprintln!("{}", msg);
        process::exit(1);
    }
}
