package com.craftinginterpreters.klox

sealed class Stmt {

  interface Visitor<out R> {
    fun visitBlockStmt(stmt: Block): R
//    fun visitStmt(stmt: Class): R
    fun visitExpressionStmt(stmt: Expression): R
//    fun visit(stmt: Function): R
//    fun visit(stmt: If): R
    fun visitPrintStmt(stmt: Print): R
//    fun visit(stmt: Return): R
    fun visitVarStmt(stmt: Var): R
//    fun visit(stmt: While): R
  }

  data class Block(val statements: List<Stmt>) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>) = visitor.visitBlockStmt(this)
  }

  data class Expression(val expression: Expr) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>) = visitor.visitExpressionStmt(this)
  }

  data class Print(val expression: Expr) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>) = visitor.visitPrintStmt(this)
  }

  data class Var(val name: Token, val initializer: Expr?) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>) = visitor.visitVarStmt(this)
  }

  abstract fun <R> accept(visitor: Visitor<R>): R
}