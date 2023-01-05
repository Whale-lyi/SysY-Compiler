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
   : constDecl
   | varDecl
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
   : IDENT (L_BRACKT constExp R_BRACKT)* (ASSIGN initVal)?
   ;

initVal
   : exp                                            # ExpInitVal
   | L_BRACE (initVal (COMMA initVal)*)? R_BRACE    # ArrayInitVal
   ;

funcDef
   : funcType IDENT L_PAREN (funcFParams)? R_PAREN block
   ;

funcType
   : VOID
   | INT
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
   | lhs = exp (op = MUL | op = DIV | op = MOD) rhs = exp   # MulDivModExp
   | lhs = exp (op = PLUS | op = MINUS) rhs = exp           # AddSubExp
   ;

cond
   : exp                                                            # ExpCond
   | lhs = cond (op = LT | op = GT | op = LE | op = GE) rhs = cond  # LTGTLEGECond
   | lhs = cond (op = EQ | op = NEQ) rhs = cond                     # EQNEQCond
   | lhs = cond AND rhs = cond                                      # AndCond
   | lhs = cond OR rhs = cond                                       # OrCond
   ;

lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

number
   : INTEGR_CONST
   ;

unaryOp
   : PLUS
   | MINUS
   | NOT
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