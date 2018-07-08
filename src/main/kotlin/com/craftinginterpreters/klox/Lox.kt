package com.craftinginterpreters.klox

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

val interpreter = Interpreter()
var hadError = false
var hadRuntimeError = false

@Throws(IOException::class)
fun main(args: Array<String>) {
  when {
    args.size > 1 -> System.out.println("Usage: klox [script]")
    args.size == 1 -> runFile(args[0])
    else -> runPrompt()
  }
}

@Throws(IOException::class)
private fun runFile(path: String) {
  val bytes = Files.readAllBytes(Paths.get(path))
  run(String(bytes, Charset.defaultCharset()))

  if (hadError) System.exit(65)
  if (hadRuntimeError) System.exit(70)
}

@Throws(IOException::class)
private fun runPrompt() {
  val input = InputStreamReader(System.`in`)
  val reader = BufferedReader(input)

  while (true) {
    System.out.print("> ")
    run(reader.readLine())
    hadError = false
  }
}

private fun run(source: String) {
  val scanner = Scanner(source)
  val tokens = scanner.scanTokens()
  val parser = Parser(tokens)
  val statements = parser.parse()

  // parse error
  if (hadError) return

  val resolver = Resolver(interpreter)

  // resolution error
  if (hadError) return

  resolver.resolve(statements)

  interpreter.interpret(statements)
}

fun error(line: Int, message: String) {
  report(line, "", message)
}

fun error(token: Token, message: String) {
  when {
    token.type == TokenType.EOF -> report(token.line, " at end", message)
    else -> report(token.line, " at '" + token.lexeme + "'", message)
  }
}

fun runtimeError(error: RuntimeError) {
  System.err.println(error.message + "\n[line " + error.token.line + "]")
  hadRuntimeError = true
}

private fun report(line: Int, where: String, message: String) {
  System.err.println("[line $line] Error $where: $message")
  hadError = true
}