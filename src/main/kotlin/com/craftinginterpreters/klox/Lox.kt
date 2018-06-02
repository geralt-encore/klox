package com.craftinginterpreters.klox

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

var hadError = false

@Throws(IOException::class)
fun main(args: Array<String>) {
  when {
    args.size > 1 -> System.out.println("Usage: klox [script]")
    args.size == 1 -> runFile(args[0])
    else -> runPrompt()
  }
}

fun error(line: Int, message: String) {
  report(line, "", message)
}

@Throws(IOException::class)
private fun runFile(path: String) {
  val bytes = Files.readAllBytes(Paths.get(path))
  run(String(bytes, Charset.defaultCharset()))

  if (hadError) System.exit(65)
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

  tokens.forEach { System.out.println(it) }
}

private fun report(line: Int, where: String, message: String) {
  System.err.println("[line $line] Error $where: $message")
  hadError = true
}