# LSG announcements

## Dependencies

On Debian-ish systems...

### TeX

```
sudo apt install texlive texlive-xetex texlive-lang-polish texlive-fonts-extra
```

### Inkscape

```
sudo apt install inkscape
```

### Pdfunite

```
sudo apt install poppler-utils
```

### Scala scripting

Tested with Ammonite 2.5.5, install:

	curl -L https://github.com/lihaoyi/ammonite/releases/download/2.5.5/2.13-2.5.5 > amm
	chmod +x amm && mv amm ~/.local/bin/

## Use

There's a messy makefile, doesn't have make all, run the targets separately.
