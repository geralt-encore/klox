package com.craftinginterpreters.klox

import com.craftinginterpreters.klox.error as loxError


class Parser(private val tokens: List<Token>) {

  private var current = 0

  fun parse(): List<Stmt> {
    val statements = mutableListOf<Stmt?>()
    while (!isAtEnd()) {
      statements += declaration()
    }
    return statements.filterNotNull()
  }

  private fun declaration(): Stmt? {
    try {
      if (match(TokenType.FUN)) return function("function")
      if (match(TokenType.VAR)) return varDeclaration()
      return statement()
    } catch (error: ParseError) {
      synchronize()
      return null
    }
  }

  private fun function(kind: String): Stmt {
    val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")

    consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
    val parameters = mutableListOf<Token>()
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size >= 8) {
          error(peek(), "Cannot have more than 8 parameters.")
        }
        parameters += consume(TokenType.IDENTIFIER, "Expect parameter name.")
      } while (match(TokenType.COMMA))
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")

    consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
    val body = block()
    return Stmt.Function(name, parameters, body)
  }

  private fun varDeclaration(): Stmt {
    val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

    var initializer: Expr? = null
    if (match(TokenType.EQUAL)) {
      initializer = expression()
    }

    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
    return Stmt.Var(name, initializer)
  }

  private fun statement() = when {
    match(TokenType.FOR) -> forStatement()
    match(TokenType.IF) -> ifStatement()
    match(TokenType.PRINT) -> printStatement()
    match(TokenType.RETURN) -> returnStatement()
    match(TokenType.WHILE) -> whileStatement()
    match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
    else -> expressionStatement()
  }

  private fun forStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

    val initializer = when {
      match(TokenType.SEMICOLON) -> null
      match(TokenType.VAR) -> varDeclaration()
      else -> expressionStatement()
    }

    var condition: Expr? = null
    if (!check(TokenType.SEMICOLON)) {
      condition = expression()
    }
    consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

    var increment: Expr? = null
    if (!check(TokenType.RIGHT_PAREN)) {
      increment = expression()
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

    var body = statement()

    if (increment != null) {
      body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
    }

    if (condition == null) condition = Expr.Literal(true)
    body = Stmt.While(condition, body)

    if (initializer != null) {
      body = Stmt.Block(listOf(initializer, body))
    }

    return body
  }

  private fun ifStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
    val condition = expression()
    consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")

    val thenBranch = statement()
    val elseBranch = if (match(TokenType.ELSE)) statement() else null

    return Stmt.If(condition, thenBranch, elseBranch)
  }

  private fun printStatement(): Stmt {
    val value = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after value.")
    return Stmt.Print(value)
  }

  private fun returnStatement(): Stmt {
    val keyword = previous()
    var value: Expr? = null
    if (!check(TokenType.SEMICOLON)) {
      value = expression()
    }

    consume(TokenType.SEMICOLON, "Expect ';' after return value.")
    return Stmt.Return(keyword, value)
  }

  private fun whileStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
    val condition = expression()
    consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
    val body = statement()

    return Stmt.While(condition, body)
  }

  private fun block(): List<Stmt> {
    val statements = mutableListOf<Stmt?>()

    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration())
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
    return statements.filterNotNull()
  }

  private fun expressionStatement(): Stmt {
    val expr = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after expression.")
    return Stmt.Expression(expr)
  }

  private fun expression() = assignment()

  private fun assignment(): Expr {
    val expr = or()

    if (match(TokenType.EQUAL)) {
      val equals = previous()
      val value = assignment()

      if (expr is Expr.Variable) {
        val name = expr.name
        return Expr.Assign(name, value)
      }

      error(equals, "Invalid assignment target.")
    }

    return expr
  }

  private fun or(): Expr {
    var expr = and()

    while (match(TokenType.OR)) {
      val operator = previous()
      val right = and()
      expr = Expr.Logical(expr, operator, right)
    }

    return expr
  }

  private fun and(): Expr {
    var expr = equality()

    while (match(TokenType.AND)) {
      val operator = previous()
      val right = equality()
      expr = Expr.Logical(expr, operator, right)
    }

    return expr
  }

  private fun equality(): Expr {
    var expr = comparison()

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      val operator = previous()
      val right = comparison()
      expr = Expr.Binary(expr, operator, right)
    }

    return expr
  }

  private fun comparison(): Expr {
    var expr = addition()

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      val operator = previous()
      val right = addition()
      expr = Expr.Binary(expr, operator, right)
    }

    return expr
  }

  private fun addition(): Expr {
    var expr = multiplication()

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      val operator = previous()
      val right = multiplication()
      expr = Expr.Binary(expr, operator, right)
    }

    return expr
  }

  private fun multiplication(): Expr {
    var expr = unary()

    while (match(TokenType.SLASH, TokenType.STAR)) {
      val operator = previous()
      val right = unary()
      expr = Expr.Binary(expr, operator, right)
    }

    return expr
  }

  private fun unary(): Expr {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      val operator = previous()
      val right = unary()
      return Expr.Unary(operator, right)
    }

    return call()
  }

  private fun call(): Expr {
    var expr = primary()

    while (true) {
      if (match(TokenType.LEFT_PAREN)) {
        expr = finishCall(expr)
      } else {
        break
      }
    }

    return expr
  }

  private fun finishCall(callee: Expr): Expr {
    val arguments = mutableListOf<Expr>()
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (arguments.size >= 8) {
          error(peek(), "Cannot have more than 8 arguments.")
        }
        arguments += expression()
      } while (match(TokenType.COMMA))
    }

    val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
    return Expr.Call(callee, paren, arguments)
  }

  private fun primary(): Expr {
    if (match(TokenType.FALSE)) return Expr.Literal(false)
    if (match(TokenType.TRUE)) return Expr.Literal(true)
    if (match(TokenType.NIL)) return Expr.Literal(null)

    if (match(TokenType.NUMBER, TokenType.STRING)) {
      return Expr.Literal(previous().literal)
    }

    if (match(TokenType.IDENTIFIER)) {
      return Expr.Variable(previous())
    }

    if (match(TokenType.LEFT_PAREN)) {
      val expr = expression()
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
      return Expr.Grouping(expr)
    }

    throw error(peek(), "Expect expression.")
  }

  private fun consume(type: TokenType, message: String): Token {
    if (check(type)) return advance()

    throw error(peek(), message)
  }

  private fun error(token: Token, message: String): ParseError {
    loxError(token, message)

    return ParseError()
  }

  private fun match(vararg types: TokenType): Boolean {
    types.forEach { type ->
      if (check(type)) {
        advance()
        return true
      }
    }

    return false
  }

  private fun check(tokenType: TokenType): Boolean {
    if (isAtEnd()) return false

    return peek().type == tokenType
  }

  private fun advance(): Token {
    if (!isAtEnd()) current++

    return previous()
  }

  private fun previous() = tokens[current - 1]

  private fun isAtEnd() = peek().type === TokenType.EOF

  private fun peek() = tokens[current]

  private fun synchronize() {
    advance()

    while (!isAtEnd()) {
      if (previous().type === TokenType.SEMICOLON) return

      @Suppress("NON_EXHAUSTIVE_WHEN")
      when (peek().type) {
        TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR,
        TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
      }

      advance()
    }
  }

  class ParseError : RuntimeException()
}