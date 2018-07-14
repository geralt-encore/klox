package com.craftinginterpreters.klox

class LoxInstance(private val klass: LoxClass) {

  private val fields = mutableMapOf<String, Any?>()

  fun get(name: Token): Any? {
    if (fields.containsKey(name.lexeme)) {
      return fields[name.lexeme]
    }

    val method = klass.findMethod(this, name.lexeme)
    if (method != null) return method

    throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
  }

  fun set(name: Token, value: Any?) {
    fields[name.lexeme] = value
  }

  override fun toString() = "${klass.name} instance"
}