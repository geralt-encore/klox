package com.craftinginterpreters.klox

class LoxClass(val name: String,
               private val superclass: LoxClass?,
               private val methods: Map<String, LoxFunction>) : LoxCallable {

  fun findMethod(instance: LoxInstance, name: String): LoxFunction? {
    if (methods.containsKey(name)) {
      return methods[name]!!.bind(instance)
    }

    if (superclass != null) {
      return superclass.findMethod(instance, name)
    }

    return null
  }

  override fun call(interpreter: Interpreter, arguments: List<Any?>): LoxInstance {
    val instance = LoxInstance(this)

    val initializer = methods["init"]
    initializer?.bind(instance)?.call(interpreter, arguments)

    return instance
  }

  override fun arity(): Int {
    val initializer = methods["init"] ?: return 0
    return initializer.arity()
  }

  override fun toString() = name
}