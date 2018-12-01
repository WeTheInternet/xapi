/* Generated By:JavaCC: Do not edit this line. ASTParserConstants.java */
/*
 *
 * This file is part of Java 1.8 parser and Abstract Syntax Tree.
 *
 * Java 1.8 parser and Abstract Syntax Tree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java 1.8 parser and Abstract Syntax Tree.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.javaparser;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface ASTParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int SINGLE_LINE_COMMENT = 6;
  /** RegularExpression Id. */
  int JAVA_DOC_COMMENT = 9;
  /** RegularExpression Id. */
  int MULTI_LINE_COMMENT = 10;
  /** RegularExpression Id. */
  int UI_COMMENT_BODY = 11;
  /** RegularExpression Id. */
  int END_UI_COMMENT = 12;
  /** RegularExpression Id. */
  int END_UI_BODY = 14;
  /** RegularExpression Id. */
  int UI_COMMENT = 15;
  /** RegularExpression Id. */
  int UI_TEXT = 16;
  /** RegularExpression Id. */
  int HEX_COLOR = 17;
  /** RegularExpression Id. */
  int CSS_KEY = 18;
  /** RegularExpression Id. */
  int CSS_SELECTOR = 19;
  /** RegularExpression Id. */
  int CSS_SELECTOR_JOIN = 20;
  /** RegularExpression Id. */
  int START_CSS_BLOCK = 21;
  /** RegularExpression Id. */
  int START_CSS_INLINE = 22;
  /** RegularExpression Id. */
  int IMPORTANT = 23;
  /** RegularExpression Id. */
  int CSS_UNIT = 24;
  /** RegularExpression Id. */
  int ABSTRACT = 25;
  /** RegularExpression Id. */
  int ASSERT = 26;
  /** RegularExpression Id. */
  int BOOLEAN = 27;
  /** RegularExpression Id. */
  int BREAK = 28;
  /** RegularExpression Id. */
  int BYTE = 29;
  /** RegularExpression Id. */
  int CASE = 30;
  /** RegularExpression Id. */
  int CATCH = 31;
  /** RegularExpression Id. */
  int CHAR = 32;
  /** RegularExpression Id. */
  int CLASS = 33;
  /** RegularExpression Id. */
  int CONST = 34;
  /** RegularExpression Id. */
  int CONTINUE = 35;
  /** RegularExpression Id. */
  int _DEFAULT = 36;
  /** RegularExpression Id. */
  int DO = 37;
  /** RegularExpression Id. */
  int DOUBLE = 38;
  /** RegularExpression Id. */
  int ELSE = 39;
  /** RegularExpression Id. */
  int ENUM = 40;
  /** RegularExpression Id. */
  int EXTENDS = 41;
  /** RegularExpression Id. */
  int FALSE = 42;
  /** RegularExpression Id. */
  int FINAL = 43;
  /** RegularExpression Id. */
  int FINALLY = 44;
  /** RegularExpression Id. */
  int FLOAT = 45;
  /** RegularExpression Id. */
  int FOR = 46;
  /** RegularExpression Id. */
  int GOTO = 47;
  /** RegularExpression Id. */
  int IF = 48;
  /** RegularExpression Id. */
  int IMPLEMENTS = 49;
  /** RegularExpression Id. */
  int IMPORT = 50;
  /** RegularExpression Id. */
  int INSTANCEOF = 51;
  /** RegularExpression Id. */
  int INT = 52;
  /** RegularExpression Id. */
  int INTERFACE = 53;
  /** RegularExpression Id. */
  int LONG = 54;
  /** RegularExpression Id. */
  int NATIVE = 55;
  /** RegularExpression Id. */
  int NEW = 56;
  /** RegularExpression Id. */
  int NULL = 57;
  /** RegularExpression Id. */
  int PACKAGE = 58;
  /** RegularExpression Id. */
  int PRIVATE = 59;
  /** RegularExpression Id. */
  int PROTECTED = 60;
  /** RegularExpression Id. */
  int PUBLIC = 61;
  /** RegularExpression Id. */
  int RETURN = 62;
  /** RegularExpression Id. */
  int SHORT = 63;
  /** RegularExpression Id. */
  int STATIC = 64;
  /** RegularExpression Id. */
  int STRICTFP = 65;
  /** RegularExpression Id. */
  int SUPER = 66;
  /** RegularExpression Id. */
  int SWITCH = 67;
  /** RegularExpression Id. */
  int SYNCHRONIZED = 68;
  /** RegularExpression Id. */
  int THIS = 69;
  /** RegularExpression Id. */
  int THROW = 70;
  /** RegularExpression Id. */
  int THROWS = 71;
  /** RegularExpression Id. */
  int TRANSIENT = 72;
  /** RegularExpression Id. */
  int TRUE = 73;
  /** RegularExpression Id. */
  int TRY = 74;
  /** RegularExpression Id. */
  int VOID = 75;
  /** RegularExpression Id. */
  int VOLATILE = 76;
  /** RegularExpression Id. */
  int WHILE = 77;
  /** RegularExpression Id. */
  int DOCTYPE = 78;
  /** RegularExpression Id. */
  int BEGIN_EXPR = 79;
  /** RegularExpression Id. */
  int BEGIN_DECL = 80;
  /** RegularExpression Id. */
  int START_JAVA = 81;
  /** RegularExpression Id. */
  int END_JAVA = 82;
  /** RegularExpression Id. */
  int START_HTML = 83;
  /** RegularExpression Id. */
  int END_HTML = 84;
  /** RegularExpression Id. */
  int START_CSS = 85;
  /** RegularExpression Id. */
  int END_CSS = 86;
  /** RegularExpression Id. */
  int START_PI = 87;
  /** RegularExpression Id. */
  int END_PI = 88;
  /** RegularExpression Id. */
  int START_UI = 89;
  /** RegularExpression Id. */
  int START_SCRIPT = 90;
  /** RegularExpression Id. */
  int END_SCRIPT = 91;
  /** RegularExpression Id. */
  int LONG_LITERAL = 92;
  /** RegularExpression Id. */
  int INTEGER_LITERAL = 93;
  /** RegularExpression Id. */
  int DECIMAL_LITERAL = 94;
  /** RegularExpression Id. */
  int HEX_LITERAL = 95;
  /** RegularExpression Id. */
  int OCTAL_LITERAL = 96;
  /** RegularExpression Id. */
  int BINARY_LITERAL = 97;
  /** RegularExpression Id. */
  int FLOATING_POINT_LITERAL = 98;
  /** RegularExpression Id. */
  int DECIMAL_FLOATING_POINT_LITERAL = 99;
  /** RegularExpression Id. */
  int DECIMAL_EXPONENT = 100;
  /** RegularExpression Id. */
  int HEXADECIMAL_FLOATING_POINT_LITERAL = 101;
  /** RegularExpression Id. */
  int HEXADECIMAL_EXPONENT = 102;
  /** RegularExpression Id. */
  int CHARACTER_LITERAL = 103;
  /** RegularExpression Id. */
  int STRING_LITERAL = 104;
  /** RegularExpression Id. */
  int TEMPLATE_LITERAL = 105;
  /** RegularExpression Id. */
  int IDENTIFIER = 106;
  /** RegularExpression Id. */
  int FEATURE_IDENTIFIER = 107;
  /** RegularExpression Id. */
  int LETTER = 108;
  /** RegularExpression Id. */
  int PART_LETTER = 109;
  /** RegularExpression Id. */
  int LPAREN = 110;
  /** RegularExpression Id. */
  int RPAREN = 111;
  /** RegularExpression Id. */
  int LBRACE = 112;
  /** RegularExpression Id. */
  int RBRACE = 113;
  /** RegularExpression Id. */
  int LBRACKET = 114;
  /** RegularExpression Id. */
  int RBRACKET = 115;
  /** RegularExpression Id. */
  int SEMICOLON = 116;
  /** RegularExpression Id. */
  int COMMA = 117;
  /** RegularExpression Id. */
  int DOT = 118;
  /** RegularExpression Id. */
  int AT = 119;
  /** RegularExpression Id. */
  int ASSIGN = 120;
  /** RegularExpression Id. */
  int LT = 121;
  /** RegularExpression Id. */
  int BANG = 122;
  /** RegularExpression Id. */
  int TILDE = 123;
  /** RegularExpression Id. */
  int HOOK = 124;
  /** RegularExpression Id. */
  int COLON = 125;
  /** RegularExpression Id. */
  int EQ = 126;
  /** RegularExpression Id. */
  int LE = 127;
  /** RegularExpression Id. */
  int GE = 128;
  /** RegularExpression Id. */
  int NE = 129;
  /** RegularExpression Id. */
  int SC_OR = 130;
  /** RegularExpression Id. */
  int SC_AND = 131;
  /** RegularExpression Id. */
  int INCR = 132;
  /** RegularExpression Id. */
  int DECR = 133;
  /** RegularExpression Id. */
  int PLUS = 134;
  /** RegularExpression Id. */
  int MINUS = 135;
  /** RegularExpression Id. */
  int STAR = 136;
  /** RegularExpression Id. */
  int SLASH = 137;
  /** RegularExpression Id. */
  int BIT_AND = 138;
  /** RegularExpression Id. */
  int BIT_OR = 139;
  /** RegularExpression Id. */
  int XOR = 140;
  /** RegularExpression Id. */
  int REM = 141;
  /** RegularExpression Id. */
  int LSHIFT = 142;
  /** RegularExpression Id. */
  int PLUSASSIGN = 143;
  /** RegularExpression Id. */
  int MINUSASSIGN = 144;
  /** RegularExpression Id. */
  int STARASSIGN = 145;
  /** RegularExpression Id. */
  int SLASHASSIGN = 146;
  /** RegularExpression Id. */
  int ANDASSIGN = 147;
  /** RegularExpression Id. */
  int ORASSIGN = 148;
  /** RegularExpression Id. */
  int XORASSIGN = 149;
  /** RegularExpression Id. */
  int REMASSIGN = 150;
  /** RegularExpression Id. */
  int LSHIFTASSIGN = 151;
  /** RegularExpression Id. */
  int RSIGNEDSHIFTASSIGN = 152;
  /** RegularExpression Id. */
  int RUNSIGNEDSHIFTASSIGN = 153;
  /** RegularExpression Id. */
  int ELLIPSIS = 154;
  /** RegularExpression Id. */
  int ARROW = 155;
  /** RegularExpression Id. */
  int DOUBLECOLON = 156;
  /** RegularExpression Id. */
  int RUNSIGNEDSHIFT = 157;
  /** RegularExpression Id. */
  int RSIGNEDSHIFT = 158;
  /** RegularExpression Id. */
  int GT = 159;

  /** Lexical state. */
  int DEFAULT = 0;
  /** Lexical state. */
  int CSS_BODY = 1;
  /** Lexical state. */
  int UI_BODY = 2;
  /** Lexical state. */
  int JSON_BODY = 3;
  /** Lexical state. */
  int IN_JAVA_DOC_COMMENT = 4;
  /** Lexical state. */
  int IN_MULTI_LINE_COMMENT = 5;
  /** Lexical state. */
  int UI_COMMENT_MODE = 6;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\f\"",
    "<SINGLE_LINE_COMMENT>",
    "<token of kind 7>",
    "\"/*\"",
    "\"*/\"",
    "\"*/\"",
    "<UI_COMMENT_BODY>",
    "\"-->\"",
    "<token of kind 13>",
    "\"</\"",
    "\"<!--\"",
    "<UI_TEXT>",
    "<HEX_COLOR>",
    "<CSS_KEY>",
    "<CSS_SELECTOR>",
    "<CSS_SELECTOR_JOIN>",
    "\".{\"",
    "\":{\"",
    "\"!important\"",
    "<CSS_UNIT>",
    "\"abstract\"",
    "\"assert\"",
    "\"boolean\"",
    "\"break\"",
    "\"byte\"",
    "\"case\"",
    "\"catch\"",
    "\"char\"",
    "\"class\"",
    "\"const\"",
    "\"continue\"",
    "\"default\"",
    "\"do\"",
    "\"double\"",
    "\"else\"",
    "\"enum\"",
    "\"extends\"",
    "\"false\"",
    "\"final\"",
    "\"finally\"",
    "\"float\"",
    "\"for\"",
    "\"goto\"",
    "\"if\"",
    "\"implements\"",
    "\"import\"",
    "\"instanceof\"",
    "\"int\"",
    "\"interface\"",
    "\"long\"",
    "\"native\"",
    "\"new\"",
    "\"null\"",
    "\"package\"",
    "\"private\"",
    "\"protected\"",
    "\"public\"",
    "\"return\"",
    "\"short\"",
    "\"static\"",
    "\"strictfp\"",
    "\"super\"",
    "\"switch\"",
    "\"synchronized\"",
    "\"this\"",
    "\"throw\"",
    "\"throws\"",
    "\"transient\"",
    "\"true\"",
    "\"try\"",
    "\"void\"",
    "\"volatile\"",
    "\"while\"",
    "<DOCTYPE>",
    "\"$.\"",
    "\"$:\"",
    "\"`java:\"",
    "\":java`\"",
    "\"`html:\"",
    "\":html`\"",
    "\"`css:\"",
    "\":css`\"",
    "\"`@\"",
    "\"@`\"",
    "\"`<\"",
    "\"`{\"",
    "\"}`\"",
    "<LONG_LITERAL>",
    "<INTEGER_LITERAL>",
    "<DECIMAL_LITERAL>",
    "<HEX_LITERAL>",
    "<OCTAL_LITERAL>",
    "<BINARY_LITERAL>",
    "<FLOATING_POINT_LITERAL>",
    "<DECIMAL_FLOATING_POINT_LITERAL>",
    "<DECIMAL_EXPONENT>",
    "<HEXADECIMAL_FLOATING_POINT_LITERAL>",
    "<HEXADECIMAL_EXPONENT>",
    "<CHARACTER_LITERAL>",
    "<STRING_LITERAL>",
    "<TEMPLATE_LITERAL>",
    "<IDENTIFIER>",
    "<FEATURE_IDENTIFIER>",
    "<LETTER>",
    "<PART_LETTER>",
    "\"(\"",
    "\")\"",
    "\"{\"",
    "\"}\"",
    "\"[\"",
    "\"]\"",
    "\";\"",
    "\",\"",
    "\".\"",
    "\"@\"",
    "\"=\"",
    "\"<\"",
    "\"!\"",
    "\"~\"",
    "\"?\"",
    "\":\"",
    "\"==\"",
    "\"<=\"",
    "\">=\"",
    "\"!=\"",
    "\"||\"",
    "\"&&\"",
    "\"++\"",
    "\"--\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "\"&\"",
    "\"|\"",
    "\"^\"",
    "\"%\"",
    "\"<<\"",
    "\"+=\"",
    "\"-=\"",
    "\"*=\"",
    "\"/=\"",
    "\"&=\"",
    "\"|=\"",
    "\"^=\"",
    "\"%=\"",
    "\"<<=\"",
    "\">>=\"",
    "\">>>=\"",
    "\"...\"",
    "\"->\"",
    "\"::\"",
    "\">>>\"",
    "\">>\"",
    "\">\"",
    "\"\\u001a\"",
    "\"\\\\\\\\\"",
    "\">`\"",
    "\"\\\\\"",
    "\"</\"",
    "\">:\"",
  };

}
