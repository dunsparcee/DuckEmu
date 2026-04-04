# DuckEmu: NES (Nintendo Entertainment System) Technical Reference

This document provides a highly detailed technical reference for the **NES (Nintendo Entertainment System)** implementation in DuckEmu, covering the hardware architecture, memory mappings, and internal code logic.

---

## 1. System Architecture: Ricoh 2A03 (6502)

The NES CPU is based on the MOS Technology 6502 core, customized by Ricoh for Nintendo.

### **1.1. Registers**
- **8-bit Registers**: `A` (Accumulator), `X`, `Y`, `S` (Stack Pointer).
- **16-bit Register**: `PC` (Program Counter).
- **Flags (`F`)**:
    - **Carry (C)**: Set if a carry occurred.
    - **Zero (Z)**: Set if the result is zero.
    - **Interrupt Disable (I)**: Mask maskable interrupts.
    - **Decimal (D)**: Ignored on NES, but present as a flag.
    - **Break (B)**: Used by software to signal a breakpoint.
    - **Overflow (V)**: Set if a bitwise operation caused a signed overflow.
    - **Negative (N)**: Set if the MSB (bit 7) is 1.

### **1.2. Execution Model**
Implemented in `Cpu.kt`.
- **Clock**: 1.789773 MHz (NTSC) / 1.662607 MHz (PAL).
- **Syncing**: DuckEmu uses a cycle-counted implementation (`exec(clk)`) to coordinate the CPU and PPU states.

---

## 2. Memory Maps & Mirroring

The NES uses separate address spaces for the CPU and the PPU.

### **2.1. CPU Memory Map**
| Range | Name | Description |
| :--- | :--- | :--- |
| **$0000–$07FF** | Internal RAM | 2KB. Mirrored at $0800, $1000, and $1800. |
| **$2000–$2007** | PPU Registers | 8 Registers. Mirrored every 8 bytes from $2008 to $3FFF. |
| **$4000–$4017** | APU/IO Registers | Sound, DMA, and Controller ports. |
| **$4020–$5FFF** | Expansion ROM | Varies by cartridge Mapper. |
| **$6000–$7FFF** | Work RAM (SRAM) | Cartridge RAM, used for saves. |
| **$8000–$FFFF** | PRG-ROM | 32KB space, managed by Mappers. |

### **2.2. PPU Memory Map**
| Range | Name | Description |
| :--- | :--- | :--- |
| **$0000–$1FFF** | Pattern Tables | Two sets of 256 tiles each (often ROM). |
| **$2000–$2FFF** | Nametables | Background layout. 4 tables of 1KB each. |
| **$3000–$3EFF** | Mirror of $2000–2EFF | |
| **$3F00–$3F1F** | Palettes | 32 entries for BG and Sprites. |

---

## 3. PPU (Picture Processing Unit) - 2C02

The NES PPU is a scanline-based renderer implemented in `Ppu.kt`.

### **3.1. Rendering Components**
- **Nametables**: 32x30 grid of 8-bit tile indices.
- **Attribute Tables**: 64-byte sections at the end of each nametable defining palettes for 16x16 tile blocks.
- **OAM**: 256-byte internal buffer for 64 sprites (handled by `renderSPR()`).
- **Palettes**: 54/64 total possible colors, with 32 active palette entries.
- **Mirroring**: Supports Horizontal, Vertical, 4-Screen, and Single-Screen mirroring via `setMirroring()`.

### **3.2. Sprite 0 Hit Logic**
Critical for many NES games' split-screen effects. DuckEmu implements this in `spriteCheck()`, which detects if Sprite 0 overlaps with a non-zero background pixel on the current scanline.

---

## 4. APU (Audio Processing Unit)

DuckEmu implements the 2A03 APU in `Apu.kt`.

### **4.1. Channels**
- **Pulse 1 & 2**: Two square wave channels with variable duty cycles and sweeps.
- **Triangle**: 4-bit, 32-step resolution wave for basslines.
- **Noise**: LFSR-based generator with two modes (32767 vs 93 steps).
- **DMC**: Plays Delta Modulation samples by DMA-reading PRG-ROM.

---

## 5. Mappers & Implementation Details

NES carts frequently include mappers to bank-switch PRG and CHR data. These are implemented in the `io.duckemu.nes.domain.mapper` package.

### **Common Mappers Implemented:**
- **NROM (Mapper 0)**: Simple mirroring of the internal RAM.
- **MMC1 (Mapper 1)**: Supports bank switching for PRG and CHR via bit-serial writes.
- **MMC3 (Mapper 4)**: Includes a scanline-based IRQ counter (`hblank(line)` in `MMC3.kt`).
- **CNROM / UNROM**: Basic switching based on latch writes.
- **VRC6 (Mapper 24)**: Konami custom chip adding extra pulse and sawtooth audio channels.

---

## 6. System Integration Logic

### **`Nes.kt` Core Logic**
The `Nes` class acts as the central hub:
- **`execFrame()`**: Drives a full 262-line frame loop.
    - Synchronizes `cpu.exec(114)` cycles for each scanline.
    - Triggers `ppu.render(line)` for visible lines (0–239).
    - Signals V-Blank and handles CPU NMI.

### **`Mbc.kt` (Memory Bank Controller)**
The bus implementor. It decodes all memory requests from the CPU and PPU and routes them to the appropriate component (RAM, PPU registers, ROM banks).

### **Interrupt Vectors**
- **$FFFA**: NMI (Non-Maskable Interrupt). Typically used for V-Blank start.
- **$FFFC**: RESET. Starting address for CPU execution.
- **$FFFE**: IRQ/BRK. Standard maskable interrupt.

---

