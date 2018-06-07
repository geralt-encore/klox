package com.craftinginterpreters.klox

import com.craftinginterpreters.klox.TokenType.AND
import com.craftinginterpreters.klox.TokenType.BANG
import com.craftinginterpreters.klox.TokenType.BANG_EQUAL
import com.craftinginterpreters.klox.TokenType.CLASS
import com.craftinginterpreters.klox.TokenType.COMMA
import com.craftinginterpreters.klox.TokenType.DOT
import com.craftinginterpreters.klox.TokenType.ELSE
import com.craftinginterpreters.klox.TokenType.EOF
import com.craftinginterpreters.klox.TokenType.EQUAL
import com.craftinginterpreters.klox.TokenType.EQUAL_EQUAL
import com.craftinginterpreters.klox.TokenType.FALSE
import com.craftinginterpreters.klox.TokenType.FOR
import com.craftinginterpreters.klox.TokenType.FUN
import com.craftinginterpreters.klox.TokenType.GREATER
import com.craftinginterpreters.klox.TokenType.GREATER_EQUAL
import com.craftinginterpreters.klox.TokenType.IDENTIFIER
import com.craftinginterpreters.klox.TokenType.IF
import com.craftinginterpreters.klox.TokenType.LEFT_BRACE
import com.craftinginterpreters.klox.TokenType.LEFT_PAREN
import com.craftinginterpreters.klox.TokenType.LESS
import com.craftinginterpreters.klox.TokenType.LESS_EQUAL
import com.craftinginterpreters.klox.TokenType.MINUS
import com.craftinginterpreters.klox.TokenType.NIL
import com.craftinginterpreters.klox.TokenType.NUMBER
import com.craftinginterpreters.klox.TokenType.OR
import com.craftinginterpreters.klox.TokenType.PLUS
import com.craftinginterpreters.klox.TokenType.PRINT
import com.craftinginterpreters.klox.TokenType.RETURN
import com.craftinginterpreters.klox.TokenType.RIGHT_BRACE
import com.craftinginterpreters.klox.TokenType.RIGHT_PAREN
import com.craftinginterpreters.klox.TokenType.SEMICOLON
import com.craftinginterpreters.klox.TokenType.SLASH
import com.craftinginterpreters.klox.TokenType.STAR
import com.craftinginterpreters.klox.TokenType.STRING
import com.craftinginterpreters.klox.TokenType.SUPER
import com.craftinginterpreters.klox.TokenType.THIS
import com.craftinginterpreters.klox.TokenType.TRUE
import com.craftinginterpreters.klox.TokenType.VAR
import com.craftinginterpreters.klox.TokenType.WHILE


private val keywords = mapOf(
    "and" to AND,
    "class" to CLASS,
    "else" to ELSE,
    "false" to FALSE,
    "for" to FOR,
    "fun" to FUN,
    "if" to IF,
    "nil" to NIL,
    "or" to OR,
    "print" to PRINT,
    "return" to RETURN,
    "super" to SUPER,
    "this" to THIS,
    "true" to TRUE,
    "var" to VAR,
    "while" to WHILE
)

class Scanner(private val source: String) {

  private val tokens = mutableListOf<Token>()
  private var start = 0
  private var current = 0
  private var line = 1

  fun scanTokens(): List<Token> {
    while (!isAtEnd()) {
      start = current
      scanToken()
    }

    tokens += Token(EOF, "", null, line)
    return tokens
  }

  private fun scanToken() {
    val c = advance()

    when (c) {
      '(' -> addToken(LEFT_PAREN)
      ')' -> addToken(RIGHT_PAREN)
      '{' -> addToken(LEFT_BRACE)
      '}' -> addToken(RIGHT_BRACE)
      ',' -> addToken(COMMA)
      '.' -> addToken(DOT)
      '-' -> addToken(MINUS)
      '+' -> addToken(PLUS)
      ';' -> addToken(SEMICOLON)
      '*' -> addToken(STAR)
      '!' -> if (match('=')) addToken(BANG_EQUAL) else addToken(BANG)
      '=' -> if (match('=')) addToken(EQUAL_EQUAL) else addToken(EQUAL)
      '<' -> if (match('=')) addToken(LESS_EQUAL) else addToken(LESS)
      '>' -> if (match('=')) addToken(GREATER_EQUAL) else addToken(GREATER)
      '/' -> {
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) advance()
        } else {
          addToken(SLASH)
        }
      }
      ' ', '\r', '\t' -> {}
      '\n' -> line++
      '"' -> string()
      else -> {
        when {
          isDigit(c) -> number()
          isAlpha(c) -> identifier()
          else -> error(line, "Unexpected character.")
        }
      }
    }
  }

  private fun addToken(type: TokenType, literal: Any?) {
    val text = source.substring(start, current)
    tokens += Token(type, text, literal, line)
  }

  private fun addToken(type: TokenType) {
    addToken(type, null)
  }

  private fun isAtEnd() = current >= source.length

  private fun advance() = source[++current - 1]

  private fun match(expected: Char): Boolean {
    if (isAtEnd()) return false
    if (source[current] != expected) return false

    current++
    return true
  }

  private fun peek(): Char {
    if (isAtEnd()) return '\u0000'
    return source[current]
  }

  private fun peekNext() = if (current + 1 >= source.length) '\u0000' else source[current + 1]

  private fun string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++
      advance()
    }

    if (isAtEnd()) {
      error(line, "Unterminated string.")
      return
    }

    advance()

    val value = source.substring(start + 1, current - 1)
    addToken(STRING, value)
  }

  private fun number() {
    while (isDigit(peek())) advance()

    if (peek() == '.' && isDigit(peekNext())) {
      advance()

      while (isDigit(peek())) advance()
    }

    addToken(NUMBER, (source.substring(start, current).toDouble()))
  }

  private fun identifier() {
    while (isAlphaNumeric(peek())) advance()

    val text = source.substring(start, current)
    val type = keywords[text] ?: IDENTIFIER
    addToken(type)
  }

  private fun isDigit(c: Char) = c in '0'..'9'

  private fun isAlpha(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c == '_'

  private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)
}