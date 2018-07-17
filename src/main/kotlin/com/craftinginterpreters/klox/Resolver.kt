package com.craftinginterpreters.klox

import java.util.*


class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

  private val scopes: Stack<MutableMap<String, Boolean>> = Stack()
  private var currentFunction = FunctionType.NONE
  private var currentClass = ClassType.NONE

  fun resolve(statements: List<Stmt>) {
    statements.forEach { resolve(it) }
  }

  override fun visitBlockStmt(stmt: Stmt.Block) {
    beginScope()
    resolve(stmt.statements)
    endScope()
  }

  override fun visitVarStmt(stmt: Stmt.Var) {
    declare(stmt.name)
    if (stmt.initializer != null) {
      resolve(stmt.initializer)
    }
    define(stmt.name)
  }

  override fun visitVariableExpr(expr: Expr.Variable) {
    if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
      error(expr.name, "Cannot read local variable in its own initializer.")
    }

    resolveLocal(expr, expr.name)
  }

  override fun visitAssignExpr(expr: Expr.Assign) {
    resolve(expr.value)
    resolveLocal(expr, expr.name)
  }

  override fun visitClassStmt(stmt: Stmt.Class) {
    val enclosingClass = currentClass
    currentClass = ClassType.CLASS

    declare(stmt.name)

    if (stmt.superclass != null) {
      currentClass = ClassType.SUBCLASS
      resolve(stmt.superclass)
    }

    define(stmt.name)

    if (stmt.superclass != null) {
      beginScope()
      scopes.peek()["super"] = true
    }

    beginScope()
    scopes.peek()["this"] = true

    stmt.methods.forEach { method ->
      var declaration = FunctionType.METHOD
      if (method.name.lexeme == "init") {
        declaration = FunctionType.INITIALIZER
      }
      resolveFunction(method, declaration)
    }

    endScope()

    if (stmt.superclass != null) endScope()

    currentClass = enclosingClass
  }

  override fun visitFunctionStmt(stmt: Stmt.Function) {
    declare(stmt.name)
    define(stmt.name)

    resolveFunction(stmt, FunctionType.FUNCTION)
  }

  override fun visitGetExpr(expr: Expr.Get) {
    resolve(expr.obj)
  }

  override fun visitSetExpr(expr: Expr.Set) {
    resolve(expr.value)
    resolve(expr.obj)
  }

  override fun visitThisExpr(expr: Expr.This) {
    if (currentClass == ClassType.NONE) {
      error(expr.keyword, "Cannot use 'this' outside of a class.")
    }
    resolveLocal(expr, expr.keyword)
  }

  override fun visitSuperExpr(expr: Expr.Super) {
    when {
      currentClass == ClassType.NONE -> error(expr.keyword, "Cannot use 'super' outside of a class.")
      currentClass != ClassType.SUBCLASS -> error(expr.keyword, "Cannot use 'super' in a class with no superclass.")
    }

    resolveLocal(expr, expr.keyword)
  }

  override fun visitExpressionStmt(stmt: Stmt.Expression) {
    resolve(stmt.expression)
  }

  override fun visitIfStmt(stmt: Stmt.If) {
    resolve(stmt.condition)
    resolve(stmt.thenBranch)
    if (stmt.elseBranch != null) resolve(stmt.elseBranch)
  }

  override fun visitPrintStmt(stmt: Stmt.Print) {
    resolve(stmt.expression)
  }

  override fun visitReturnStmt(stmt: Stmt.Return) {
    if (currentFunction == FunctionType.NONE) {
      error(stmt.keyword, "Cannot return from top-level code.")
    }
    if (stmt.value != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        error(stmt.keyword, "Cannot return a value from an initializer.")
      }

      resolve(stmt.value)
    }
  }

  override fun visitWhileStmt(stmt: Stmt.While) {
    resolve(stmt.condition)
    resolve(stmt.body)
  }


  override fun visitBinaryExpr(expr: Expr.Binary) {
    resolve(expr.left)
    resolve(expr.right)
  }

  override fun visitCallExpr(expr: Expr.Call) {
    resolve(expr.callee)

    expr.arguments.forEach { resolve(it) }

  }

  override fun visitGroupingExpr(expr: Expr.Grouping) {
    resolve(expr.expression)
  }

  override fun visitLiteralExpr(expr: Expr.Literal) {
  }

  override fun visitLogicalExpr(expr: Expr.Logical) {
    resolve(expr.left)
    resolve(expr.right)
  }

  override fun visitUnaryExpr(expr: Expr.Unary) {
    resolve(expr.right)
  }

  private fun beginScope() {
    scopes.push(mutableMapOf())
  }

  private fun endScope() {
    scopes.pop()
  }

  private fun resolve(statement: Stmt) {
    statement.accept(this)
  }

  private fun resolve(expr: Expr) {
    expr.accept(this)
  }

  private fun declare(name: Token) {
    if (scopes.isEmpty()) return

    val scope = scopes.peek()
    if (scope.containsKey(name.lexeme)) {
      error(name, "Variable with this name already declared in this scope.")
    }
    scope[name.lexeme] = false
  }

  private fun define(name: Token) {
    if (scopes.isEmpty()) return
    scopes.peek()[name.lexeme] = true
  }

  private fun resolveLocal(expr: Expr, name: Token) {
    for (i in scopes.size - 1 downTo 0) {
      if (scopes[i].containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size - 1 - i)
        return
      }
    }
  }

  private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
    val enclosingFunction = currentFunction
    currentFunction = type

    beginScope()
    function.parameters.forEach {
      declare(it)
      define(it)
    }
    resolve(function.body)
    endScope()
    currentFunction = enclosingFunction
  }

  private enum class FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD
  }

  private enum class ClassType {
    NONE,
    CLASS,
    SUBCLASS
  }
}