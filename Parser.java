import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL

 Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'

 Lexical:   id is a single character; 
 int_lit is an unsigned integer;
 equality operator is =, not ==

 Sample Program: Factorial

 int n, i, f;
 n = 4;
 i = 1;
 f = 1;
 while (i < n) {
 i = i + 1;
 f= f * i;
 }
 end

 Sample Program:  GCD

 int x, y;
 x = 121;
 y = 132;
 while (x != y) {
 if (x > y) 
 { x = x - y; }
 else { y = y - x; }
 }
 end

 */

public class Parser {
	public static int indexCompare = 0;
	public static int indexif = 0;
	public static String str = null;
	public static String strWhile = null;
	public static int indexelse = 0;
	public static int returnIndex = 0;
	public static int conditionCount = 0;
	public static int loopCount = 0;
	public static int condPointer = 0;
	public static int elseCount = 0;
	public static HashMap<Character, Integer> hm = new HashMap<Character, Integer>();
	public static Stack<Integer> stackIf = new Stack<Integer>();
	public static Stack<String> stackStrWhile = new Stack<String>();
	public static Stack<Integer> stackWhile = new Stack<Integer>();
	public static int gotoIndex = 0;
	public static boolean gotoFlag = false;
	public static boolean ifFlag = false;
	public static int indexwhile = 0;

	public static void main(String[] args) {
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex();
		Program p = new Program();
		Code.output();
	}
}

class Program // program -> decls stmts end
{

	Decls dec_Program;
	Stmts stmts_Program;

	public Program() {

		dec_Program = new Decls();
		stmts_Program = new Stmts();
		Code.gen("return");

	}

}

class Decls // decls -> int idlist ;
{
	Idlist idlist;

	public Decls() {
		if (Lexer.nextToken == Token.KEY_INT) {
			Lexer.lex();
			idlist = new Idlist();

		}

	}
}

class Idlist // idlist -> id { , id }
{

	public Idlist() {
		int count = 0;
		while (Lexer.nextToken != Token.SEMICOLON) {
			if (Lexer.nextToken != Token.COMMA) {
				Parser.hm.put(Lexer.ident, count);
				count++;
			}
			Lexer.lex();
			if (Lexer.nextToken == Token.SEMICOLON
					|| Lexer.nextToken == Token.KEY_END) {
				break;
			}

		}

		Lexer.lex();

	}
}

class Stmt // assign | cond | loop
{

	Assign a_stmt;
	Cond conIF;
	Cond conElse;
	Loop loopWhile;

	public Stmt() {
		if ((Lexer.nextToken == Token.LEFT_PAREN)
				|| (Lexer.nextToken == Token.COMMA)
				|| (Lexer.nextToken == Token.LEFT_BRACE)
				|| (Lexer.nextToken == Token.RIGHT_PAREN)) {
			Lexer.lex();
		}

		switch (Lexer.nextToken) {
		case Token.ID: // assign operator
			a_stmt = new Assign();
			break;
		case Token.KEY_IF: // '('
			conIF = new Cond();
			break;
		case Token.KEY_ELSE:
			conElse = new Cond();
			break;
		case Token.KEY_WHILE:
			loopWhile = new Loop();
			break;

		default:
			break;
		}

	}

}

class Stmts // stmts -> stmt [ stmts ]
{
	Stmts stmts;
	Stmt stmt_stmts;

	public Stmts() {
		stmt_stmts = new Stmt();

		if ((Lexer.nextToken == Token.SEMICOLON)) {
			Lexer.lex();
		}
		if (Lexer.nextToken == Token.RIGHT_BRACE) {
			Lexer.lex();
			if (Lexer.nextToken == Token.KEY_END) {
			} else {
				return;
			}
		} else if (Lexer.nextToken != Token.KEY_END) {
			stmts = new Stmts();
		}

	}

}

class Assign // assign -> id = expr ;
{
	char id;
	Expr e_Assign1;
	Expr e_Assign2;

	public Assign() {
		id = Lexer.ident;
		Lexer.lex();
		if (Lexer.nextToken == Token.ASSIGN_OP) {
			Lexer.lex();
			e_Assign1 = new Expr();

			while (Lexer.nextToken != Token.SEMICOLON) {
				e_Assign2 = new Expr();
				if (Lexer.nextToken == Token.SEMICOLON) {
					break;
				}
				Lexer.lex();
			}
		}
		if (Parser.hm.containsKey(id)) {
			Code.gen("istore_" + (Parser.hm.get(id)));
		}

	}
}

class Cond {
	Cmpdstmt cmpt_Cond;
	Rexpr rexp_Cond;
	Cmpdstmt cmp_Condelse;

	public Cond() {
		if (Lexer.nextToken == Token.KEY_IF) {
			Lexer.lex();
			Lexer.lex();
			rexp_Cond = new Rexpr();
			Parser.indexif = Code.codeptr;
			Parser.stackIf.push(Parser.indexif);
			Lexer.lex();
			Parser.str = rexp_Cond.condition + " ";
			Code.gen(Parser.str + Code.codeptr);
			Code.gen("");
			Code.gen("");
			cmpt_Cond = new Cmpdstmt();
			if (Lexer.nextToken == Token.KEY_ELSE) {
				Parser.indexelse = Code.codeptr;
				Parser.stackIf.push(Code.codeptr);
				Parser.elseCount = 1;
				Code.gen("goto" + " " + Code.codeptr);
				Code.gen("");
				Code.gen("");
				Lexer.lex();
				cmp_Condelse = new Cmpdstmt();
				if (Lexer.nextToken != Token.KEY_END) {
					int t = Parser.stackIf.pop();
					int n = Parser.stackIf.pop();
					Code.code[t] = "goto" + " " + Code.codeptr;
					Code.code[n] = Parser.str + (Parser.indexelse + 3);
				} else {
					int t = Parser.stackIf.pop();
					int n = Parser.stackIf.pop();
					Code.code[t] = "goto" + " " + Code.codeptr;
					Code.code[n] = Parser.str + (t + 3);
				}

			} else {
				int p = Parser.stackIf.pop();
				Code.code[p] = Parser.str + Code.codeptr;
			}
		}

	}
}

class Loop // loop -> while '(' rexp ')' cmpdstmt
{
	Rexpr rexp_Loop;
	Cmpdstmt cmpd_Loop;

	public Loop() {
		Parser.loopCount = 1;
		if (Lexer.nextToken == Token.KEY_WHILE) {
			Lexer.lex();
			Lexer.lex();
			Parser.condPointer = Code.codeptr;
			Parser.stackWhile.push(Code.codeptr);
			rexp_Loop = new Rexpr();
			Parser.indexwhile = Code.codeptr;
			Parser.stackWhile.push(Code.codeptr);
			Lexer.lex();
			Parser.strWhile = rexp_Loop.condition + " ";
			Parser.stackStrWhile.push(Parser.strWhile);
			Code.gen(Parser.strWhile + Code.codeptr);
			Code.gen("");
			Code.gen("");
			cmpd_Loop = new Cmpdstmt();
			String temp = Parser.stackStrWhile.pop();
			int f = Parser.stackWhile.pop();
			int g = Parser.stackWhile.pop();
			Code.gen("goto" + " " + g);
			Code.gen("");
			Code.gen("");
			Code.code[f] = temp + Code.codeptr;
		}

	}
}

class Cmpdstmt // cmpdstmt-> '{' stmts '}'
{
	Stmts stmt_Cmpd1;
	Stmts stmt_Cmpd2;

	public Cmpdstmt() {
		if (Lexer.nextToken == Token.ID) {
			stmt_Cmpd1 = new Stmts();
		}

		else if (Lexer.nextToken == Token.LEFT_BRACE) {
			stmt_Cmpd2 = new Stmts();
		}
		if (Lexer.nextToken == Token.RIGHT_BRACE
				|| Lexer.nextToken == Token.KEY_ELSE) {
			return;
		} else {
		}

	}
}

class Rexpr // rexp -> expr (< | > | =) expr
{
	char op;
	Expr ex_2;
	String condition = new String();
	Expr expr_Rexpr1;
	Expr expr_Rexpr2;
	Expr expr_Rexpr3;

	public Rexpr() {
		expr_Rexpr1 = new Expr();
		if (Lexer.nextToken == Token.LESSER_OP) {
			condition = "if_icmpge";
		} else if (Lexer.nextToken == Token.GREATER_OP) {
			condition = "if_icmple";
		} else if (Lexer.nextToken == Token.NOT_EQ) {
			condition = "if_icmpeq";
		} else if (Lexer.nextToken == Token.ASSIGN_OP) {
			condition = "if_icmpne";
		}
		Lexer.lex();
		expr_Rexpr2 = new Expr();
		if (Lexer.nextToken == Token.ASSIGN_OP) {
			condition = "if_icmpne";
			Lexer.lex();
			expr_Rexpr3 = new Expr();
			Lexer.lex();
		}
	}

}

// code

class Expr // expr -> term [ (+ | -) expr ]
{
	char op_expr;
	Term term_Expr;
	Expr expr_Expr;

	public Expr() {
		if (Lexer.nextToken == Token.LEFT_PAREN) {
			Lexer.lex();
		}
		term_Expr = new Term();

		if (Lexer.nextToken != Token.SEMICOLON) {
			if (Lexer.nextToken == Token.ADD_OP
					|| Lexer.nextToken == Token.SUB_OP) {
				op_expr = Lexer.nextChar;
				Lexer.lex();
				expr_Expr = new Expr();
				Code.gen(Code.opcode(op_expr));
			}
		}
	}

}

class Term {

	char op_Term;
	Factor fact_Term;
	Expr expr_Term;
	Term term_Term;

	public Term() { // factor [ (* | /) term ]
		fact_Term = new Factor();
		if (Lexer.nextToken != Token.SEMICOLON) {

			if (Lexer.nextToken == Token.MULT_OP
					|| Lexer.nextToken == Token.DIV_OP) {
				op_Term = Lexer.nextChar;
				Lexer.lex();
				if (Lexer.nextToken == Token.LEFT_PAREN) {
					expr_Term = new Expr();
					if (Lexer.nextToken == Token.RIGHT_PAREN) {
						Lexer.lex();

						Code.gen(Code.opcode(op_Term));
					}

				} else {
					term_Term = new Term();
					Code.gen(Code.opcode(op_Term));
				}

			}

		}

	}
}

class Factor {
	int i = 0;
	char id;
	Expr expr_Factor;

	public Factor() {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			if (i < 6) {
				Lexer.lex();
				Code.gen("iconst_" + i);
			} else if (i < 128) {
				Lexer.lex();
				Code.gen("bipush " + i);
				Code.gen("");
			} else {
				Lexer.lex();
				Code.gen("sipush " + i);
				Code.gen("");
				Code.gen("");
			}

			break;

		case Token.ID:

			if (Parser.hm.containsKey(Lexer.ident)) {
				int index = Parser.hm.get(Lexer.ident);
				Code.gen("iload_" + index);
			}
			Lexer.lex();

			break;
		case Token.LEFT_BRACE:
			expr_Factor = new Expr();
			Lexer.lex();
			break;
		default:
			break;
		}
	}

}

class Code {
	static String[] code = new String[100];
	static int codeptr = 0;
	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}

	public static String opcode(char op) {
		switch (op) {
		case '+':
			return "iadd";
		case '-':
			return "isub";
		case '*':
			return "imul";
		case '/':
			return "idiv";
		default:
			return "";
		}
	}

	public static void output() {
		for (int i = 0; i < codeptr; i++)
			if (code[i] != "") {
				System.out.println(i + ": " + code[i]);
			}
	}

}
