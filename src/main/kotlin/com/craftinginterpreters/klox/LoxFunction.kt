package com.craftinginterpreters.klox

class LoxFunction(private val declaration: Stmt.Function,
                  private val closure: Environment) : LoxCallable {

  override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
    val environment = Environment(closure)
    for (i in 0 until declaration.parameters.size) {
      environment.define(declaration.parameters[i].lexeme, arguments[i])
    }

    try {
      interpreter.executeBlock(declaration.body, environment)
    } catch (returnValue: Return) {
      return returnValue.value
    }
    return null
  }

  override fun arity() = declaration.parameters.size

  override fun toString() = "<fn ${declaration.name.lexeme}>"
}