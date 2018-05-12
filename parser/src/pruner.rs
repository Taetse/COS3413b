use parser::{ConcreteNode, Node, NonTerminal, Token};

#[derive(Debug)]
pub enum AbstractNode<'a> {
    AddExpr,
    AndExpr,
    Assign,
    BoolDecl(&'a str),
    Call(&'a str),
    Code,
    CondBranch,
    EqExpr,
    False,
    ForLoop,
    GreaterExpr,
    Halt,
    Input,
    LessExpr,
    MultExpr,
    NotExpr,
    Number(&'a str),
    NumDecl(&'a str),
    OrExpr,
    Output,
    Proc(&'a str),
    ProcDefs,
    Prog,
    StrDecl(&'a str),
    String(&'a str),
    SubExpr,
    True,
    Var(&'a str),
    WhileLoop,
}
use self::AbstractNode::*;

fn make_node<'a>(
    info: AbstractNode<'a>,
    children: Vec<Box<Node<AbstractNode<'a>>>>,
) -> Box<Node<AbstractNode<'a>>> {
    Box::new(Node { info, children })
}

pub fn prune<'a>(tree: &Node<ConcreteNode<'a>>) -> Box<Node<AbstractNode<'a>>> {
    let mut n;
    match tree.info {
        ConcreteNode::Term(t) => {
            n = make_node(
                match t {
                    Token::Number(s) => Number(s),
                    Token::Special(_) => Halt,
                    Token::String(s) => String(&s[1..s.len() - 1]),
                    _ => unreachable!(),
                },
                Vec::new(),
            );
        }
        ConcreteNode::NonTerm(t) => match t {
            NonTerminal::Prog => {
                n = make_node(Prog, vec![prune(&tree.children[0])]);
                if tree.children.len() >= 3 {
                    n.children.push(prune(&tree.children[2]));
                }
            }
            NonTerminal::ProcDefs => {
                n = make_node(ProcDefs, vec![prune(&tree.children[0])]);
                let mut child = tree;
                while child.children.len() >= 2 {
                    child = &child.children[1];
                    n.children.push(prune(&child.children[0]));
                }
            }
            NonTerminal::Proc => {
                n = make_node(
                    Proc(tree.children[1].token()),
                    vec![prune(&tree.children[3])]
                );
            }
            NonTerminal::Code => {
                n = make_node(Code, vec![prune(&tree.children[0])]);
                let mut child = tree;
                while child.children.len() >= 3 {
                    child = &child.children[2];
                    n.children.push(prune(&child.children[0]));
                }
            }
            NonTerminal::Instr => n = prune(&tree.children[0]),
            NonTerminal::IO => {
                n = make_node(
                    match tree.children[0].token() {
                        "input" => Input,
                        "output" => Output,
                        _ => unreachable!(),
                    },
                    vec![prune(&tree.children[2])],
                );
            }
            NonTerminal::Call => {
                n = make_node(Call(tree.children[0].token()), Vec::new());
            }
            NonTerminal::Decl => {
                n = make_node(
                    match tree.children[0].children[0].token() {
                        "num" => NumDecl,
                        "string" => StrDecl,
                        "bool" => BoolDecl,
                        _ => unreachable!(),
                    }(tree.children[1].children[0].token()),
                    Vec::new(),
                )
            }
            NonTerminal::Var => n = make_node(Var(tree.children[0].token()), Vec::new()),
            NonTerminal::Assign => {
                n = make_node(
                    Assign,
                    vec![prune(&tree.children[0]), prune(&tree.children[2])],
                );
            }
            NonTerminal::NumExpr => n = prune(&tree.children[0]),
            NonTerminal::Calc => {
                n = make_node(
                    match tree.children[0].token() {
                        "add" => AddExpr,
                        "sub" => SubExpr,
                        "mult" => MultExpr,
                        _ => unreachable!(),
                    },
                    vec![prune(&tree.children[2]), prune(&tree.children[4])],
                );
            }
            NonTerminal::CondBranch => {
                n = make_node(
                    CondBranch,
                    vec![prune(&tree.children[2]), prune(&tree.children[6])],
                );
                if tree.children.len() >= 11 {
                    n.children.push(prune(&tree.children[10]));
                }
            }
            NonTerminal::Bool => {
                n = match tree.children[0].token() {
                    "eq" => make_node(
                        EqExpr,
                        vec![prune(&tree.children[2]), prune(&tree.children[4])],
                    ),
                    "(" => make_node(
                        match tree.children[2].token() {
                            "<" => LessExpr,
                            ">" => GreaterExpr,
                            _ => unreachable!(),
                        },
                        vec![prune(&tree.children[1]), prune(&tree.children[3])],
                    ),
                    "not" => make_node(NotExpr, vec![prune(&tree.children[1])]),
                    "and" => make_node(
                        AndExpr,
                        vec![prune(&tree.children[2]), prune(&tree.children[4])],
                    ),
                    "or" => make_node(
                        OrExpr,
                        vec![prune(&tree.children[2]), prune(&tree.children[4])],
                    ),
                    "T" => make_node(True, Vec::new()),
                    "F" => make_node(False, Vec::new()),
                    _ => prune(&tree.children[0]),
                };
            }
            NonTerminal::CondLoop => {
                n = match tree.children[0].token() {
                    "while" => make_node(
                        WhileLoop,
                        vec![prune(&tree.children[2]), prune(&tree.children[5])],
                    ),
                    "for" => make_node(
                        ForLoop,
                        vec![
                            make_node(
                                Assign,
                                vec![prune(&tree.children[2]), prune(&tree.children[4])],
                            ),
                            make_node(
                                LessExpr,
                                vec![prune(&tree.children[6]), prune(&tree.children[8])],
                            ),
                            make_node(
                                Assign,
                                vec![
                                    prune(&tree.children[10]),
                                    make_node(
                                        AddExpr,
                                        vec![prune(&tree.children[14]), prune(&tree.children[16])],
                                    ),
                                ],
                            ),
                            prune(&tree.children[20]),
                        ],
                    ),
                    _ => unreachable!(),
                };
            }
            _ => unreachable!(),
        },
    }
    n
}
