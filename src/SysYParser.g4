parser grammar SysYParser;

options {
    tokenVocab = SysYLexer;
}

program
   : compUnit
   ;

compUnit
   : (funcDef | decl)+ EOF
   ;

decl
   : constDecl  # ConstDeclStat
   | varDecl    # VarDeclStat
   ;

constDecl
   : CONST bType constDef (COMMA constDef)* SEMICOLON
   ;

bType
   : INT
   ;

constDef
   : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal
   ;

constInitVal
   : constExp                                               # ExpConstInitVal
   | L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE  # ArrayConstInitVal
   ;

varDecl
   : bType varDef (COMMA varDef)* SEMICOLON
   ;

varDef
   : IDENT (L_BRACKT constExp R_BRACKT)*                    # VarDefWithAssign
   | IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN initVal     # VarDefWithoutAssign
   ;

initVal
   : exp                                            # ExpInitVal
   | L_BRACE (initVal (COMMA initVal)*)? R_BRACE    # ArrayInitVal
   ;

funcDef
   : funcType IDENT L_PAREN (funcFParams)? R_PAREN block
   ;

funcType
   : VOID   # Void
   | INT    # Int
   ;

funcFParams
   : funcFParam (COMMA funcFParam)*
   ;

funcFParam
   : bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?
   ;

block
   : L_BRACE (blockItem)* R_BRACE
   ;

blockItem
   : decl   # DeclStat
   | stmt   # Stat
   ;

stmt
   : lVal ASSIGN exp SEMICOLON                  # AssignStat
   | (exp)? SEMICOLON                           # ExpStat
   | block                                      # BlockStat
   | IF L_PAREN cond R_PAREN stmt (ELSE stmt)?  # IfStat
   | WHILE L_PAREN cond R_PAREN stmt            # WhileStat
   | BREAK SEMICOLON                            # BreakStat
   | CONTINUE SEMICOLON                         # ContinueStat
   | RETURN (exp)? SEMICOLON                    # ReturnStat
   ;

exp
   : L_PAREN exp R_PAREN                        # ParenExp
   | lVal                                       # LeftValExp
   | number                                     # IntegerExp
   | IDENT L_PAREN funcRParams? R_PAREN         # FuncCallExp
   | unaryOp exp                                # UnaryOpExp
   | lhs = exp (op = MUL | op = DIV | op = MOD) lhs = exp   # MulDivModExp
   | lhs = exp (op = PLUS | op = MINUS) lhs = exp           # AddSubExp
   ;

cond
   : exp                                                # ExpCond
   | cond (op = LT | op = GT | op = LE | op = GE) cond  # LTGTLEGECond
   | cond (op = EQ | op = NEQ) cond                     # EQNEQCond
   | cond AND cond                                      # AndCond
   | cond OR cond                                       # OrCond
   ;

lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

number
   : INTEGR_CONST
   ;

unaryOp
   : PLUS   # Plus
   | MINUS  # Minus
   | NOT    # Not
   ;

funcRParams
   : param (COMMA param)*
   ;

param
   : exp
   ;

constExp
   : exp
   ;