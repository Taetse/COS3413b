#[derive(Clone, Copy, Debug)]
pub enum Token<'a> {
    Arithmetic(&'a str),
    Assignment(&'a str),
    Boolean(&'a str),
    Comparison(&'a str),
    Control(&'a str),
    End(&'a str),
    Grouping(&'a str),
    IO(&'a str),
    Name(&'a str),
    Number(&'a str),
    Procedure(&'a str),
    Special(&'a str),
    String(&'a str),
    Truth(&'a str),
    Type(&'a str),
}
use self::Token::*;

#[derive(Clone, Copy, Debug)]
pub enum NonTerminal {
    Assign,
    Bool,
    Calc,
    Call,
    Code,
    CondBranch,
    CondLoop,
    Decl,
    Instr,
    IO,
    Name,
    NumExpr,
    Proc,
    ProcDefs,
    Prog,
    Type,
    Var,
}

#[derive(Debug)]
pub enum ConcreteNode<'a> {
    Term(Token<'a>),
    NonTerm(NonTerminal),
}

pub struct Node<N> {
    pub info: N,
    pub children: Vec<Box<Node<N>>>,
}

impl<'a> Node<ConcreteNode<'a>> {
    pub fn token(&self) -> &'a str {
        match self.info {
            ConcreteNode::Term(t) => match t {
                Arithmetic(s) => s,
                Assignment(s) => s,
                Boolean(s) => s,
                Comparison(s) => s,
                Control(s) => s,
                End(s) => s,
                Grouping(s) => s,
                IO(s) => s,
                Name(s) => s,
                Number(s) => s,
                Procedure(s) => s,
                Special(s) => s,
                String(s) => s,
                Truth(s) => s,
                Type(s) => s,
            },
            _ => "",
        }
    }

    fn new(class: NonTerminal, children: Vec<Box<Self>>) -> Box<Self> {
        Box::new(Self {
            info: ConcreteNode::NonTerm(class),
            children,
        })
    }
}

pub struct Error<'a> {
    pub token: Token<'a>,
    pub index: usize,
}

type ParseResult<'a> = Result<Box<Node<ConcreteNode<'a>>>, Error<'a>>;

pub fn parse<'a, I: IntoIterator<Item = Token<'a>>>(tokens: I) -> ParseResult<'a> {
    let mut iter = tokens.into_iter();
    Parser {
        current: iter.next().unwrap_or(End("")),
        next: iter.next().unwrap_or(End("")),
        rest: iter,
        index: 0,
    }.parse()
}

struct Parser<'a, I> {
    current: Token<'a>,
    next: Token<'a>,
    rest: I,
    index: usize,
}

macro_rules! expect {
    ($self:ident, $($pat:pat)|+) => {
        match $self.current {
            $($pat)|+ => $self.advance(),
            _ => return $self.error(),
        }
    }
}

impl<'a, I: Iterator<Item = Token<'a>>> Parser<'a, I> {
    fn advance(&mut self) -> Box<Node<ConcreteNode<'a>>> {
        let t = self.current;
        self.current = self.next;
        self.next = self.rest.next().unwrap_or(End(""));
        self.index += 1;
        Box::new(Node {
            info: ConcreteNode::Term(t),
            children: Vec::new(),
        })
    }

    fn error(&self) -> ParseResult<'a> {
        Err(Error {
            token: self.current,
            index: self.index,
        })
    }

    fn parse(&mut self) -> ParseResult<'a> {
        let n = self.parse_prog()?;
        match self.current {
            End(_) => Ok(n),
            _ => self.error(),
        }
    }

    fn parse_prog(&mut self) -> ParseResult<'a> {
        let mut n = Node::new(NonTerminal::Prog, vec![self.parse_code()?]);
        if let Grouping(";") = self.current {
            n.children.push(self.advance());
            n.children.push(self.parse_proc_defs()?);
        }
        Ok(n)
    }

    fn parse_proc_defs(&mut self) -> ParseResult<'a> {
        let mut n = Node::new(NonTerminal::ProcDefs, vec![self.parse_proc()?]);
        if let Procedure(_) = self.current {
            n.children.push(self.parse_proc_defs()?);
        }
        Ok(n)
    }

    fn parse_proc(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::Proc,
            vec![
                expect!(self, Procedure(_)),
                expect!(self, Name(_)),
                expect!(self, Grouping("{")),
                self.parse_prog()?,
                expect!(self, Grouping("}")),
            ],
        ))
    }

    fn parse_code(&mut self) -> ParseResult<'a> {
        let mut n = Node::new(NonTerminal::Code, vec![self.parse_instr()?]);
        if let Grouping(";") = self.current {
            if let Procedure(_) = self.next {
                return Ok(n);
            }
            n.children.push(self.advance());
            n.children.push(self.parse_code()?);
        }
        Ok(n)
    }

    fn parse_instr(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::Instr,
            vec![
                match self.current {
                    Special(_) => self.advance(),
                    Type(_) => self.parse_decl()?,
                    IO(_) => self.parse_io()?,
                    Name(_) => match self.next {
                        Assignment(_) => self.parse_assign()?,
                        _ => self.parse_call()?,
                    },
                    Control("if") => self.parse_cond_branch()?,
                    Control(_) => self.parse_cond_loop()?,
                    _ => return self.error(),
                },
            ],
        ))
    }

    fn parse_io(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::IO,
            vec![
                expect!(self, IO(_)),
                expect!(self, Grouping("(")),
                self.parse_var()?,
                expect!(self, Grouping(")")),
            ],
        ))
    }

    fn parse_call(&mut self) -> ParseResult<'a> {
        Ok(Node::new(NonTerminal::Call, vec![expect!(self, Name(_))]))
    }

    fn parse_decl(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::Decl,
            vec![
                self.parse_type()?,
                self.parse_name()?,
            ],
        ))
    }

    fn parse_type(&mut self) -> ParseResult<'a> {
        Ok(Node::new(NonTerminal::Type, vec![expect!(self, Type(_))]))
    }

    fn parse_name(&mut self) -> ParseResult<'a> {
        Ok(Node::new(NonTerminal::Name, vec![expect!(self, Name(_))]))
    }

    fn parse_var(&mut self) -> ParseResult<'a> {
        Ok(Node::new(NonTerminal::Var, vec![expect!(self, Name(_))]))
    }

    fn parse_assign(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::Assign,
            vec![
                self.parse_var()?,
                expect!(self, Assignment(_)),
                match self.current {
                    String(_) => self.advance(),
                    Name(_) => self.parse_var()?,
                    Number(_) | Arithmetic(_) => self.parse_num_expr()?,
                    _ => self.parse_bool()?,
                },
            ],
        ))
    }

    fn parse_num_expr(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::NumExpr,
            vec![
                match self.current {
                    Name(_) => self.parse_var()?,
                    Number(_) => self.advance(),
                    Arithmetic(_) => self.parse_calc()?,
                    _ => return self.error(),
                },
            ],
        ))
    }

    fn parse_calc(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::Calc,
            vec![
                expect!(self, Arithmetic(_)),
                expect!(self, Grouping("(")),
                self.parse_num_expr()?,
                expect!(self, Grouping(",")),
                self.parse_num_expr()?,
                expect!(self, Grouping(")")),
            ],
        ))
    }

    fn parse_cond_branch(&mut self) -> ParseResult<'a> {
        let mut n = Node::new(
            NonTerminal::CondBranch,
            vec![
                expect!(self, Control("if")),
                expect!(self, Grouping("(")),
                self.parse_bool()?,
                expect!(self, Grouping(")")),
                expect!(self, Control("then")),
                expect!(self, Grouping("{")),
                self.parse_code()?,
                expect!(self, Grouping("}")),
            ],
        );
        if let Control("else") = self.current {
            n.children.push(self.advance());
            n.children.push(expect!(self, Grouping("{")));
            n.children.push(self.parse_code()?);
            n.children.push(expect!(self, Grouping("}")));
        }
        Ok(n)
    }

    fn parse_bool(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::Bool,
            match self.current {
                Comparison("eq") => vec![
                    self.advance(),
                    expect!(self, Grouping("(")),
                    self.parse_var()?,
                    expect!(self, Grouping(",")),
                    self.parse_var()?,
                    expect!(self, Grouping(")")),
                ],
                Grouping("(") => vec![
                    self.advance(),
                    self.parse_var()?,
                    expect!(self, Comparison("<") | Comparison(">")),
                    self.parse_var()?,
                    expect!(self, Grouping(")")),
                ],
                Boolean("not") => vec![self.advance(), self.parse_bool()?],
                Boolean(_) => vec![
                    self.advance(),
                    expect!(self, Grouping("(")),
                    self.parse_bool()?,
                    expect!(self, Grouping(",")),
                    self.parse_bool()?,
                    expect!(self, Grouping(")")),
                ],
                Truth(_) => vec![self.advance()],
                Name(_) => vec![self.parse_var()?],
                _ => return self.error(),
            },
        ))
    }

    fn parse_cond_loop(&mut self) -> ParseResult<'a> {
        Ok(Node::new(
            NonTerminal::CondLoop,
            match self.current {
                Control("while") => vec![
                    self.advance(),
                    expect!(self, Grouping("(")),
                    self.parse_bool()?,
                    expect!(self, Grouping(")")),
                    expect!(self, Grouping("{")),
                    self.parse_code()?,
                    expect!(self, Grouping("}")),
                ],
                Control("for") => vec![
                    self.advance(),
                    expect!(self, Grouping("(")),
                    self.parse_var()?,
                    expect!(self, Assignment(_)),
                    expect!(self, Number("0")),
                    expect!(self, Grouping(";")),
                    self.parse_var()?,
                    expect!(self, Comparison("<")),
                    self.parse_var()?,
                    expect!(self, Grouping(";")),
                    self.parse_var()?,
                    expect!(self, Assignment(_)),
                    expect!(self, Arithmetic("add")),
                    expect!(self, Grouping("(")),
                    self.parse_var()?,
                    expect!(self, Grouping(",")),
                    expect!(self, Number("1")),
                    expect!(self, Grouping(")")),
                    expect!(self, Grouping(")")),
                    expect!(self, Grouping("{")),
                    self.parse_code()?,
                    expect!(self, Grouping("}")),
                ],
                _ => return self.error(),
            },
        ))
    }
}
