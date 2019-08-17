package melomancool.chip8

import java.io.File

import org.pcollections.PVector
import org.pcollections.TreePVector

import melomancool.chip8.util.byteToBits
import melomancool.chip8.util.filledPVector
import melomancool.chip8.util.matchOpcode
import melomancool.chip8.util.randomUByte
import melomancool.chip8.util.replaceAt
import melomancool.chip8.util.shiftLeft
import melomancool.chip8.util.toHexString

val programStart: Int = 512

data class Chip8(
    val memory: PVector<UByte> = replaceAt(filledPVector<UByte>(4096, 0u), font, 0),
    
    // 8-bit registers V0 .. VF
    val v: PVector<UByte> = filledPVector(16, 0u),

    // 16-bit register I
    val regI: UShort = 0u,

    val programCounter: UShort = programStart.toUShort(),

    val stackPointer: UByte = 0u,

    val stack: PVector<UShort> = filledPVector(16, 0u),

    val delayTimer: UByte = 0u,
    val soundTimer: UByte = 0u,

    val keyboard: PVector<Boolean> = filledPVector(16, false),

    val displayMemory: PVector<Boolean> = filledPVector(64 * 32, false)
)

fun loadRom(chip8: Chip8, filename: String): Chip8 {
    val fileBytes = TreePVector.from(
        File(filename)
            .readBytes()
            .map { it.toUByte() }
            .let { it.subList(0, minOf(it.size, chip8.memory.size - programStart)) }
    )

    return chip8.copy(
        memory = replaceAt(chip8.memory, fileBytes, programStart)
    )
}

sealed class FlowControlOpcode
sealed class RegularOpcode

sealed class MetaOpcode
data class FlowControl(val opcode: FlowControlOpcode): MetaOpcode()
data class Regular(val opcode: RegularOpcode): MetaOpcode()
data class Unknown(val unk: String): MetaOpcode()

// No op
data class SysCall(val addr: UShort): RegularOpcode()

data class Jump(val addr: UShort): FlowControlOpcode()
data class JumpPlusV0(val addr: UShort): FlowControlOpcode()
data class Call(val addr: UShort): FlowControlOpcode()
object Return: FlowControlOpcode()

data class SkipIfRegValEqual(val reg: UByte, val value: UByte): RegularOpcode()
data class SkipIfRegValNotEqual(val reg: UByte, val value: UByte): RegularOpcode()
data class SkipIfRegRegEqual(val regX: UByte, val regY: UByte): RegularOpcode()
data class SkipIfRegRegNotEqual(val regX: UByte, val regY: UByte): RegularOpcode()
data class SkipIfKeyPressed(val reg: UByte): RegularOpcode()
data class SkipIfKeyNotPressed(val reg: UByte): RegularOpcode()

data class LoadValToReg(val reg: UByte, val value: UByte): RegularOpcode()
data class LoadRegToReg(val regX: UByte, val regY: UByte): RegularOpcode()
data class LoadDelayTimerToReg(val reg: UByte): RegularOpcode()
data class LoadKeyToReg(val reg: UByte): RegularOpcode()
data class LoadRegToDelayTimer(val reg: UByte): RegularOpcode()
data class LoadRegToSoundTimer(val reg: UByte): RegularOpcode()
data class LoadValToI(val value: UShort): RegularOpcode()
data class LoadSpriteLocationToI(val reg: UByte): RegularOpcode()
data class LoadRegBcdToMem(val reg: UByte): RegularOpcode()
data class LoadRegsToMem(val n: UByte): RegularOpcode()
data class LoadMemToRegs(val n: UByte): RegularOpcode()

data class SubRegFromReg(val regX: UByte, val regY: UByte): RegularOpcode()
data class SubnRegFromReg(val regX: UByte, val regY: UByte): RegularOpcode()
data class AddValToReg(val reg: UByte, val value: UByte): RegularOpcode()
data class AddRegToI(val reg: UByte): RegularOpcode()

data class OrRegReg(val regX: UByte, val regY: UByte): RegularOpcode()
data class AndRegReg(val regX: UByte, val regY: UByte): RegularOpcode()
data class XorRegReg(val regX: UByte, val regY: UByte): RegularOpcode()
data class AddRegToReg(val regX: UByte, val regY: UByte): RegularOpcode()
data class ShiftRightReg(val reg: UByte): RegularOpcode()
data class ShiftLeftReg(val reg: UByte): RegularOpcode()
data class RandomAndValToReg(val reg: UByte, val value: UByte): RegularOpcode()

object ClearScreen: RegularOpcode()
data class DrawSprite(val regX: UByte, val regY: UByte, val n: UByte): RegularOpcode()

fun parseOpcode(byteHigh: UByte, byteLow: UByte): MetaOpcode {
    val stringOpcode = toHexString(byteHigh) + toHexString(byteLow)
    println(stringOpcode)
    
    return matchOpcode(stringOpcode) {
           "00E0" {              Regular(ClearScreen) }
        ?: "00EE" {              FlowControl(Return) }
        ?: "0nnn" { (nnn)     -> Regular(SysCall(nnn.toUShort(16))) }
        ?: "1nnn" { (nnn)     -> FlowControl(Jump(nnn.toUShort(16))) }
        ?: "2nnn" { (x)       -> FlowControl(Call(x.toUShort(16))) }
        ?: "3xkk" { (x, kk)   -> Regular(SkipIfRegValEqual(x.toUByte(16), kk.toUByte(16))) }
        ?: "4xkk" { (x, kk)   -> Regular(SkipIfRegValNotEqual(x.toUByte(16), kk.toUByte(16))) }
        ?: "5xy0" { (x, y)    -> Regular(SkipIfRegRegEqual(x.toUByte(16), y.toUByte(16))) }
        ?: "6xkk" { (x, kk)   -> Regular(LoadValToReg(x.toUByte(16), kk.toUByte(16))) }
        ?: "7xkk" { (x, kk)   -> Regular(AddValToReg(x.toUByte(16), kk.toUByte(16))) }
        ?: "8xy0" { (x, y)    -> Regular(LoadRegToReg(x.toUByte(16), y.toUByte(16))) }
        ?: "8xy1" { (x, y)    -> Regular(OrRegReg(x.toUByte(16), y.toUByte(16))) }
        ?: "8xy2" { (x, y)    -> Regular(AndRegReg(x.toUByte(16), y.toUByte(16))) }
        ?: "8xy3" { (x, y)    -> Regular(XorRegReg(x.toUByte(16), y.toUByte(16))) }
        ?: "8xy4" { (x, y)    -> Regular(AddRegToReg(x.toUByte(16), y.toUByte(16))) }
        ?: "8xy5" { (x, y)    -> Regular(SubRegFromReg(x.toUByte(16), y.toUByte(16))) }
        ?: "8xy6" { (x, _)    -> Regular(ShiftLeftReg(x.toUByte(16))) }
        ?: "8xy7" { (x, y)    -> Regular(SubnRegFromReg(x.toUByte(16), y.toUByte(16))) }
        ?: "8xyE" { (x, _)    -> Regular(ShiftRightReg(x.toUByte(16))) }
        ?: "9xy0" { (x, y)    -> Regular(SkipIfRegRegNotEqual(x.toUByte(16), y.toUByte(16))) }
        ?: "Annn" { (nnn)     -> Regular(LoadValToI(nnn.toUShort(16))) }
        ?: "Bnnn" { (nnn)     -> FlowControl(JumpPlusV0(nnn.toUShort(16))) }
        ?: "Cxkk" { (x, kk)   -> Regular(RandomAndValToReg(x.toUByte(16), kk.toUByte(16))) }
        ?: "Dxyn" { (x, y, n) -> Regular(DrawSprite(x.toUByte(16), y.toUByte(16), n.toUByte(16))) }
        ?: "Ex9E" { (x)       -> Regular(SkipIfKeyPressed(x.toUByte(16))) }
        ?: "ExA1" { (x)       -> Regular(SkipIfKeyNotPressed(x.toUByte(16))) }
        ?: "Fx07" { (x)       -> Regular(LoadDelayTimerToReg(x.toUByte(16))) }
        ?: "Fx0A" { (x)       -> Regular(LoadKeyToReg(x.toUByte(16))) }
        ?: "Fx15" { (x)       -> Regular(LoadRegToDelayTimer(x.toUByte(16))) }
        ?: "Fx18" { (x)       -> Regular(LoadRegToSoundTimer(x.toUByte(16))) }
        ?: "Fx1E" { (x)       -> Regular(AddRegToI(x.toUByte(16))) }
        ?: "Fx29" { (x)       -> Regular(LoadSpriteLocationToI(x.toUByte(16))) }
        ?: "Fx33" { (x)       -> Regular(LoadRegBcdToMem(x.toUByte(16))) }
        ?: "Fn55" { (n)       -> Regular(LoadRegsToMem(n.toUByte(16))) }
        ?: "Fn65" { (n)       -> Regular(LoadMemToRegs(n.toUByte(16))) }
        ?:                       Unknown(stringOpcode)
    }
}

fun step(chip8: Chip8): Chip8 {
    val metaOpcode = parseOpcode(
        chip8.memory[chip8.programCounter.toInt()],
        chip8.memory[chip8.programCounter.toInt() + 1]
    )
    println(metaOpcode)

    return chip8.run { when (metaOpcode) {
        is FlowControl -> metaOpcode.opcode.let { opcode -> when (opcode) {
            is Call ->
                copy(
                    stack = stack.with(stackPointer.toInt(), programCounter),
                    stackPointer = (stackPointer + 1u).toUByte(),
                    programCounter = opcode.addr
                )
            is Jump ->
                copy(programCounter = opcode.addr)
            is JumpPlusV0 ->
                copy(programCounter = (opcode.addr + v[0]).toUShort())
            Return ->
                copy(
                    programCounter = stack[stackPointer.toInt()],
                    stackPointer = (stackPointer - 1u).toUByte()
                )
        }}
        is Regular -> metaOpcode.opcode.let { opcode -> when (opcode) {
            is SysCall ->
                chip8
            is LoadValToReg ->
                copy(v = v.with(opcode.reg.toInt(), opcode.value))
            is LoadValToI ->
                copy(regI = opcode.value)
            is RandomAndValToReg ->
                copy(v = v.with(opcode.reg.toInt(), randomUByte() and opcode.value))
            is SkipIfRegValEqual ->
                if (v[opcode.reg.toInt()] == opcode.value) {
                    copy(programCounter = (programCounter + 2u).toUShort())
                } else {
                    chip8
                }
            else ->
                chip8
        }}
        .run { copy(programCounter = (programCounter + 2u).toUShort()) }
        is Unknown ->
            chip8
    }}
}

fun main() {
    Chip8()
    // .let { loadRom(it, "Pong (alt).ch8") }
    // .let { loadRom(it, "PONG") }
    .let { loadRom(it, "maze.rom") }
        .let { step(it) }
        .let { step(it) }
        .let { step(it) }
        .let { step(it) }
        .let { step(it) }
        .let { step(it) }
        .let { step(it) }

    println(byteToBits(17u))
}
