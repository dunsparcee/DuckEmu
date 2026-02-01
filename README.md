<div style="display: flex; justify-content: center;">
    <img width="128" src="https://i.imgur.com/bcBLJ20.png" style="border-radius: 12px; align: center" alt="DuckEmu-Logo"/>
</div>

## Welcome to DuckEmu.

DuckEmu is a future multiplatform GB/GBC/GBA/NDS emulator written in Kotlin.

> This emulator is work-in-process.  
> Currently, its only capable of run GB/GBC on Windows and Mac

## Screenshots

### GB

<img width="400" src="https://i.imgur.com/YobGKEX.png" alt="pokemon-red"/>

### GBC

<img width="400" src="https://i.imgur.com/aVXeXOi.png" alt="pokemon-crystal"/>
<img width="400" src="https://i.imgur.com/gnpOrk2.png" alt="pokemon-crystal"/>
<img width="400" src="https://i.imgur.com/8hamJyj.png" alt="pokemon-crystal"/>

## Usage

Download from [release](https://github.com/dunsparcee/DuckEmu/releases).

After unzipping, the contents will be as follows.

Except for GB and GBC, BIOS files are **required**

## Controls

| Type     | A            | B            | Start        | Select       | D-pad                                               |
|----------|--------------|--------------|--------------|--------------|-----------------------------------------------------|
| Keyboard | <kbd>K</kbd> | <kbd>J</kbd> | <kbd>H</kbd> | <kbd>F</kbd> | <kbd>W</kbd> <kbd>S</kbd> <kbd>A</kbd> <kbd>D</kbd> |

## Build from source

Require "Kotlin" and "Gradle"

```sh
gh repo clone dunsparcee/DuckEmu
cd DuckEmu/composeApp
```

```sh
gradle composeApp:package<DMG/Msi/Deb>
```
