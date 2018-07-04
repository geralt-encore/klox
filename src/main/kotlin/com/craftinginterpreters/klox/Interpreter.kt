package com.craftinginterpreters.klox

import com.craftinginterpreters.klox.Stmt.Return




class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

  val globals = Environment()
  private var environment = globals

  init {
    globals.define("clock", object : LoxCallable {

      override fun arity() = 0

      override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        return System.currentTimeMillis().toDouble() / 1000.0
      }
    })
  }

  fun interpret(statements: List<Stmt>) {
    try {
      statements.forEach { execute(it) }
    } catch (error: RuntimeError) {
      runtimeError(error)
    }
  }

  private fun execute(statement: Stmt) {
    statement.accept(this)
  }

  override fun visitBinaryExpr(expr: Expr.Binary): Any? {
    val left = evaluate(expr.left)
    val right = evaluate(expr.right)

    return when (expr.operator.type) {
      TokenType.GREATER -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double > right as Double
      }
      TokenType.GREATER_EQUAL -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double >= right as Double
      }
      TokenType.LESS -> {
        checkNumberOperands(expr.operator, left, right)
        (left as Double) < right as Double
      }
      TokenType.LESS_EQUAL -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double <= right as Double
      }
      TokenType.MINUS -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double - right as Double
      }
      TokenType.PLUS -> when {
        left is Double && right is Double -> left + right
        left is String && right is String -> left + right
        else -> throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
      }
      TokenType.SLASH -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double / right as Double
      }
      TokenType.STAR -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double * right as Double
      }
      TokenType.BANG_EQUAL -> !isEqual(left, right)
      TokenType.EQUAL_EQUAL -> isEqual(left, right)
      else -> null
    }
  }

  override fun visitGroupingExpr(expr: Expr.Grouping) = evaluate(expr.expression)

  override fun visitLiteralExpr(expr: Expr.Literal) = expr.value

  override fun visitUnaryExpr(expr: Expr.Unary): Any? {
    val right = evaluate(expr.right)

    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (expr.operator.type) {
      TokenType.BANG -> !isTruthy(right)
      TokenType.MINUS -> {
        checkNumberOperand(expr.operator, right)
        -(right as Double)
      }
    }

    return null
  }

  override fun visitVariableExpr(expr: Expr.Variable) = environment.get(expr.name)

  override fun visitAssignExpr(expr: Expr.Assign): Any? {
    val value = evaluate(expr.value)

    environment.assign(expr.name, value)
    return value
  }

  override fun visitLogicalExpr(expr: Expr.Logical): Any? {
    val left = evaluate(expr.left)

    when {
      expr.operator.type == TokenType.OR -> if (isTruthy(left)) return left
      expr.operator.type == TokenType.AND -> if (!isTruthy(left)) return left
    }

    return evaluate(expr.right)
  }

  override fun visitCallExpr(expr: Expr.Call): Any? {
    val callee = evaluate(expr.callee)

    val arguments = mutableListOf<Any?>()
    expr.arguments.forEach { arguments += evaluate(it) }

    if (callee !is LoxCallable) {
      throw RuntimeError(expr.paren, "Can only call functions and classes.")
    }

    val function: LoxCallable = callee
    if (arguments.size != function.arity()) {
      throw RuntimeError(expr.paren, "Expected ${function.arity()} arguments but got ${arguments.size}.")
    }

    return function.call(this, arguments)
  }

  override fun visitExpressionStmt(stmt: Stmt.Expression) {
    evaluate(stmt.expression)
  }

  override fun visitPrintStmt(stmt: Stmt.Print) {
    val value = evaluate(stmt.expression)
    println(stringify(value))
  }

  override fun visitVarStmt(stmt: Stmt.Var) {
    var value: Any? = null
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer)
    }

    environment.define(stmt.name.lexeme, value)
  }

  override fun visitBlockStmt(stmt: Stmt.Block) {
    executeBlock(stmt.statements, Environment(environment))
  }

  override fun visitIfStmt(stmt: Stmt.If) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch)
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch)
    }
  }

  override fun visitWhileStmt(stmt: Stmt.While) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body)
    }
  }

  override fun visitFunctionStmt(stmt: Stmt.Function) {
    val function = LoxFunction(stmt, environment)
    environment.define(stmt.name.lexeme, function)
  }

  override fun visitReturnStmt(stmt: Stmt.Return) {
    var value: Any? = null
    if (stmt.value != null) value = evaluate(stmt.value)

    throw Return(value)
  }

  private fun evaluate(expr: Expr) = expr.accept(this)

  fun executeBlock(statements: List<Stmt>, environment: Environment) {
    val previous = this.environment
    try {
      this.environment = environment
      statements.forEach { execute(it) }
    } finally {
      this.environment = previous
    }
  }

  private fun isTruthy(it: Any?) = when (it) {
    null -> false
    is Boolean -> it
    else -> true
  }

  private fun isEqual(a: Any?, b: Any?) = when {
    a == null && b == null -> true
    a == null -> false
    else -> a == b
  }

  private fun checkNumberOperand(operator: Token, operand: Any?) {
    if (operand is Double) return

    throw RuntimeError(operator, "Operand must be a number.")
  }

  private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
    if (left is Double && right is Double) return

    throw RuntimeError(operator, "Operands must be numbers.")
  }

  private fun stringify(any: Any?): String {
    if (any == null) return "nil"

    // Hack. Work around Java adding ".0" to integer-valued doubles.
    if (any is Double) {
      var text = any.toString()
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length - 2)
      }
      return text
    }

    return any.toString()
  }
}