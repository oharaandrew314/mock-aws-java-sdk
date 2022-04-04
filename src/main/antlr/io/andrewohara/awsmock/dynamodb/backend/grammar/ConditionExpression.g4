grammar ConditionExpression;

parse
 : expression EOF
 ;

expression
    : OPERAND COMPARATOR OPERAND
    | OPERAND BETWEEN OPERAND AND OPERAND
    | OPERAND IN '(' OPERAND (',' OPERAND)* ')'
    | function
    | expression AND expression
    | expression OR expression
    | NOT expression
    | '(' expression ')'
    ;

function
    : 'attribute_exists (' OPERAND ')'
    | 'attribute_not_exists (' OPERAND ')'
    | 'attribute_type (' OPERAND ', ' OPERAND ')'
    | 'begins_with (' OPERAND ', ' OPERAND ')'
    | 'contains ( ' OPERAND ', ' OPERAND ')'
    | 'size (' OPERAND ')'
    ;

BETWEEN     : 'BETWEEN' ;
IN          : 'IN' ;

AND         : 'AND' ;
OR          : 'OR' ;
NOT         : 'NOT' ;

COMPARATOR
    : '='
    | '<>'
    | '<'
    | '<='
    | '>'
    | '>='
    ;

WHITESPACE  : [ \r\t\u000C\n]+ -> skip;
OPERAND     :  ~[\])]+ ;


