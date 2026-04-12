# DuckEmu: Game Boy Color (GBC) Technical Reference

This document provides a highly detailed technical reference for the **Game Boy Color (GBC)** implementation in DuckEmu, covering the hardware architecture, memory mappings, and internal code logic.

---

## 1. System Architecture: Sharp SM83

The GBC CPU is a custom 8-bit processor, the Sharp SM83, which combines features from the Intel 8080 and the Zilog Z80.

### **1.1. Registers**
- **8-bit Registers**: `A` (Accumulator), `F` (Flags), `B`, `C`, `D`, `E`, `H`, `L`.
- **16-bit Registers**: `AF`, `BC`, `DE`, `HL`, `SP` (Stack Pointer), `PC` (Program Counter).
- **Flags (`F`)**:
    - **Zero (Z)**: Set if the result of an operation is zero.
    - **Subtract (N)**: Set if the last operation was a subtraction.
    - **Half-Carry (H)**: Set if a carry occurred from the lower nibble (bit 3).
    - **Carry (C)**: Set if a carry occurred from the upper nibble (bit 7).

### **1.2. Execution Model**
Implemented in `GameBoy.run()`.
- **Clock**: 4.194304 MHz (Normal) / 8.388608 MHz (Double Speed on CGB).
- **Instruction Timing**: Opcodes take between 4 and 24 cycles. DuckEmu tracks cycles via `instrCount`.

---

## 2. Memory Map & Banking

The GBC uses a 16-bit address space ($0000–$FFFF).

| Range | Name | Description |
| :--- | :--- | :--- |
| **$0000–$3FFF** | ROM Bank 00 | Always contains the first 16KB of the cartridge. |
| **$4000–$7FFF** | ROM Bank 01–NN | Switchable banks via MBC. |
| **$8000–$9FFF** | Video RAM (VRAM) | 8KB. GBC has 2 switchable banks via Register $FF4F. |
| **$A000–$BFFF** | External RAM | 8KB. Located on the cartridge, often battery-backed. |
| **$C000–$CFFF** | Work RAM (WRAM) Bank 0 | Static 4KB of internal RAM. |
| **$D000–$DFFF** | Work RAM (WRAM) Bank 1–7 | Switchable 4KB banks (CGB only) via Register $FF70. |
| **$E000–$FDFF** | Echo RAM | Mirror of $C000–$DDFF. Prohibited for most software. |
| **$FE00–$FE9F** | OAM | Sprite Attribute Table (40 sprites). |
| **$FEA0–$FEFF** | Not Usable | Empty region. |
| **$FF00–$FF7F** | I/O Registers | Hardware control registers. |
| **$FF80–$FFFE** | High RAM (HRAM) | Internal high-speed CPU RAM. |
| **$FFFF** | IE Register | Interrupt Enable Register. |

---

## 3. PPU (Picture Processing Unit)

DuckEmu implements the GBC PPU with cycle-accurate scanline transitions.

### **3.1. Rendering Modes**
- **Mode 2 (OAM Search)**: 80 cycles. PPU reads OAM to find sprites on the current scanline.
- **Mode 3 (Transfer)**: 172–289 cycles. Transfers pixels to the display.
- **Mode 0 (H-Blank)**: Remaining cycles in the scanline (up to 204 cycles).
- **Mode 1 (V-Blank)**: 10 scanlines ($LY 144–153).

### **3.2. Critical I/O Registers**
- **LCDC ($FF40)**: Master control for Tile Maps, Window, and DMG Compatibility.
- **STAT ($FF41)**: Status and interrupt selection for LCD modes.
- **SCY/SCX ($FF42/43)**: Background scroll position.
- **LY ($FF44)**: Current scanline being processed.
- **LYC ($FF45)**: LY Compare. Triggers an interrupt when LY == LYC.
- **BCPS/BCPD ($FF68/69)**: GBC Background Palettes.
- **OCPS/OCPD ($FF6A/6B)**: GBC Object (Sprite) Palettes.

---

## 4. Audio Processing Unit (APU)

Identified as `AudioInterface` in DuckEmu. Supports four independent channels:
- **CH1 (Square)**: Sweep, Envelope, and Duty cycle control.
- **CH2 (Square)**: Envelope and Duty cycle control.
- **CH3 (Wave)**: Plays arbitrary 4-bit samples from wave RAM ($FF30–$FF3F).
- **CH4 (Noise)**: White noise or metallic patterns via LFSR.

---

## 5. Memory Bank Controllers (MBC)

Supported by `GameCard.kt` and `GameBoy.cartridgeWrite()`.

- **MBC1**: Maximum of 2MB ROM and 32KB RAM.
- **MBC2**: Fixed ROM banking with internal 512x4-bit RAM.
- **MBC3**: Supports up to 2MB ROM, 32KB RAM, and a **Real-Time Clock (RTC)**.
    - DuckEmu implements `rtcSync()` to correlate the in-game clock with the system epoch.
- **MBC5**: Modern GBC mapper supporting up to 8MB ROM and 128KB RAM.

---

## 6. Implementation Reference

### **`GameBoy.kt` Key Methods**
- **`run()`**: The main instruction loop. It decodes the 256 standard and 256 extended (CB prefix) opcodes.
- **`ioWrite(num, data)`**: Decodes register writes. Handles complex hardware triggers like starting a DMA transfer or switching VRAM/WRAM banks.
- **`initiateInterrupts()`**: Synchronizes the CPU with the PPU and Timer. It calculates the `nextTimedInterrupt` based on the closest upcoming event (V-Blank, Timer overflow, etc).
- **`flatten()` / `unflatten()`**: Serializes the execution state, registers, and memory for save states.

### **Interrupt Vectors**
- **$40**: V-Blank (Highest priority).
- **$48**: LCD STAT (Modes 0, 1, 2 or LY==LYC).
- **$50**: Timer Overflow.
- **$58**: Serial Transfer Complete.
- **$60**: Joypad Input.


