package com.craftinginterpreters.klox

private val keywords = mapOf(
    "and" to TokenType.AND,
    "class" to TokenType.CLASS,
    "else" to TokenType.ELSE,
    "false" to TokenType.FALSE,
    "for" to TokenType.FOR,
    "fun" to TokenType.FUN,
    "if" to TokenType.IF,
    "nil" to TokenType.NIL,
    "or" to TokenType.OR,
    "print" to TokenType.PRINT,
    "return" to TokenType.RETURN,
    "super" to TokenType.SUPER,
    "this" to TokenType.THIS,
    "true" to TokenType.TRUE,
    "var" to TokenType.VAR,
    "while" to TokenType.WHILE
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

    tokens += Token(TokenType.EOF, "", null, line)
    return tokens
  }

  private fun scanToken() {

    when (val c = advance()) {
      '(' -> addToken(TokenType.LEFT_PAREN)
      ')' -> addToken(TokenType.RIGHT_PAREN)
      '{' -> addToken(TokenType.LEFT_BRACE)
      '}' -> addToken(TokenType.RIGHT_BRACE)
      ',' -> addToken(TokenType.COMMA)
      '.' -> addToken(TokenType.DOT)
      '-' -> addToken(TokenType.MINUS)
      '+' -> addToken(TokenType.PLUS)
      ';' -> addToken(TokenType.SEMICOLON)
      '*' -> addToken(TokenType.STAR)
      '!' -> if (match('=')) addToken(TokenType.BANG_EQUAL) else addToken(TokenType.BANG)
      '=' -> if (match('=')) addToken(TokenType.EQUAL_EQUAL) else addToken(TokenType.EQUAL)
      '<' -> if (match('=')) addToken(TokenType.LESS_EQUAL) else addToken(TokenType.LESS)
      '>' -> if (match('=')) addToken(TokenType.GREATER_EQUAL) else addToken(TokenType.GREATER)
      '/' -> {
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) advance()
        } else {
          addToken(TokenType.SLASH)
        }
      }
      ' ', '\r', '\t' -> {
      }
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
    addToken(TokenType.STRING, value)
  }

  private fun number() {
    while (isDigit(peek())) advance()

    if (peek() == '.' && isDigit(peekNext())) {
      advance()

      while (isDigit(peek())) advance()
    }

    addToken(TokenType.NUMBER, (source.substring(start, current).toDouble()))
  }

  private fun identifier() {
    while (isAlphaNumeric(peek())) advance()

    val text = source.substring(start, current)
    val type = keywords[text] ?: TokenType.IDENTIFIER
    addToken(type)
  }

  private fun isDigit(c: Char) = c in '0'..'9'

  private fun isAlpha(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c == '_'

  private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)
}