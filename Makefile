SHELL=/bin/bash

zapisy: zapisy-pdf/ogolne-zapisy-16.pdf zapisy-pdf/ogolne-zapisy-32.pdf \
	zapisy-pdf/zapisy-zapoznawczy.pdf zapisy-pdf/zapisy-maraton.pdf zapisy-pdf/zapisy-memorial.pdf \
	zapisy-pdf/zapisy-blitz.pdf zapisy-pdf/zapisy-9.pdf zapisy-pdf/zapisy-13.pdf \
	zapisy-pdf/zapisy-torus.pdf zapisy-pdf/zapisy-fantom.pdf zapisy-pdf/zapisy-rengo.pdf

zapisy-pdf/ogolne-zapisy-16.pdf: zapisy/ogolne-zapisy-16.tex
	xelatex -output-directory zapisy-pdf zapisy/ogolne-zapisy-16.tex
zapisy-pdf/ogolne-zapisy-32.pdf: zapisy/ogolne-zapisy-32.tex
	xelatex -output-directory zapisy-pdf zapisy/ogolne-zapisy-32.tex

zapisy-pdf/zapisy-zapoznawczy.pdf: zapisy/zapisy-zapoznawczy.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-zapoznawczy.tex
zapisy-pdf/zapisy-maraton.pdf: zapisy/zapisy-maraton.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-maraton.tex
zapisy-pdf/zapisy-memorial.pdf: zapisy/zapisy-memorial.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-memorial.tex

zapisy-pdf/zapisy-blitz.pdf: zapisy/zapisy-blitz.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-blitz.tex
zapisy-pdf/zapisy-9.pdf: zapisy/zapisy-9.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-9.tex
zapisy-pdf/zapisy-13.pdf: zapisy/zapisy-13.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-13.tex
zapisy-pdf/zapisy-torus.pdf: zapisy/zapisy-torus.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-torus.tex
zapisy-pdf/zapisy-fantom.pdf: zapisy/zapisy-fantom.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-fantom.tex
zapisy-pdf/zapisy-rengo.pdf: zapisy/zapisy-rengo.tex
	xelatex -output-directory zapisy-pdf zapisy/zapisy-rengo.tex

handi: handi-pdf/handi-blitz.pdf handi-pdf/handi-9.pdf handi-pdf/handi-13.pdf

handi-pdf/handi-9.pdf: handi/handi-9.tex
	xelatex -output-directory handi-pdf handi/handi-9.tex
handi-pdf/handi-13.pdf: handi/handi-13.tex
	xelatex -output-directory handi-pdf handi/handi-13.tex
handi-pdf/handi-blitz.pdf: handi/handi-blitz.tex
	xelatex -output-directory handi-pdf handi/handi-blitz.tex

drabinki: drabinki-svg \
	drabinki-pdf/single-elim-08.pdf drabinki-pdf/double-elim-08.pdf \
	drabinki-pdf/single-elim-16.pdf drabinki-pdf/double-elim-16.pdf \
	drabinki-pdf/single-elim-24.pdf drabinki-pdf/double-elim-24.pdf \
	drabinki-pdf/single-elim-32.pdf drabinki-pdf/double-elim-32.pdf \
	drabinki-pdf/single-elim-48.pdf drabinki-pdf/double-elim-48.pdf \
	drabinki-pdf/single-elim-64.pdf drabinki-pdf/double-elim-64.pdf

drabinki-svg:
	amm drabinki/drabinka.sc

drabinki-pdf/single-elim-08.pdf: drabinki-pdf/single-elim-08.svg
	inkscape drabinki-pdf/single-elim-08.svg --export-pdf=drabinki-pdf/single-elim-08.pdf
drabinki-pdf/double-elim-08.pdf: drabinki-pdf/double-elim-08.svg
	inkscape drabinki-pdf/double-elim-08.svg --export-pdf=drabinki-pdf/double-elim-08.pdf

drabinki-pdf/single-elim-16.pdf: drabinki-pdf/single-elim-16.svg
	inkscape drabinki-pdf/single-elim-16.svg --export-pdf=drabinki-pdf/single-elim-16.pdf
drabinki-pdf/double-elim-16.pdf: drabinki-pdf/double-elim-16.svg
	inkscape drabinki-pdf/double-elim-16.svg --export-pdf=drabinki-pdf/double-elim-16.pdf

drabinki-pdf/single-elim-24.pdf: drabinki-pdf/single-elim-24.svg
	inkscape drabinki-pdf/single-elim-24.svg --export-pdf=drabinki-pdf/single-elim-24.pdf
drabinki-pdf/double-elim-24.pdf: drabinki-pdf/double-elim-24-1.svg drabinki-pdf/double-elim-24-2.svg
	inkscape drabinki-pdf/double-elim-24-1.svg --export-pdf=drabinki-pdf/double-elim-24-1.tmp.pdf
	inkscape drabinki-pdf/double-elim-24-2.svg --export-pdf=drabinki-pdf/double-elim-24-2.tmp.pdf
	pdfunite drabinki-pdf/double-elim-24-1.tmp.pdf drabinki-pdf/double-elim-24-2.tmp.pdf drabinki-pdf/double-elim-24.pdf

drabinki-pdf/single-elim-32.pdf: drabinki-pdf/single-elim-32.svg
	inkscape drabinki-pdf/single-elim-32.svg --export-pdf=drabinki-pdf/single-elim-32.pdf
drabinki-pdf/double-elim-32.pdf: drabinki-pdf/double-elim-32-1.svg drabinki-pdf/double-elim-32-2.svg
	inkscape drabinki-pdf/double-elim-32-1.svg --export-pdf=drabinki-pdf/double-elim-32-1.tmp.pdf
	inkscape drabinki-pdf/double-elim-32-2.svg --export-pdf=drabinki-pdf/double-elim-32-2.tmp.pdf
	pdfunite drabinki-pdf/double-elim-32-1.tmp.pdf drabinki-pdf/double-elim-32-2.tmp.pdf drabinki-pdf/double-elim-32.pdf

drabinki-pdf/single-elim-48.pdf: drabinki-pdf/single-elim-48-1.svg drabinki-pdf/single-elim-48-2.svg
	inkscape drabinki-pdf/single-elim-48-1.svg --export-pdf=drabinki-pdf/single-elim-48-1.tmp.pdf
	inkscape drabinki-pdf/single-elim-48-2.svg --export-pdf=drabinki-pdf/single-elim-48-2.tmp.pdf
	pdfunite drabinki-pdf/single-elim-48-1.tmp.pdf drabinki-pdf/single-elim-48-2.tmp.pdf drabinki-pdf/single-elim-48.pdf
drabinki-pdf/double-elim-48.pdf: drabinki-pdf/double-elim-48-1.svg drabinki-pdf/double-elim-48-2.svg drabinki-pdf/double-elim-48-3.svg
	inkscape drabinki-pdf/double-elim-48-1.svg --export-pdf=drabinki-pdf/double-elim-48-1.tmp.pdf
	inkscape drabinki-pdf/double-elim-48-2.svg --export-pdf=drabinki-pdf/double-elim-48-2.tmp.pdf
	inkscape drabinki-pdf/double-elim-48-3.svg --export-pdf=drabinki-pdf/double-elim-48-3.tmp.pdf
	pdfunite drabinki-pdf/double-elim-48-1.tmp.pdf drabinki-pdf/double-elim-48-2.tmp.pdf drabinki-pdf/double-elim-48-3.tmp.pdf drabinki-pdf/double-elim-48.pdf

drabinki-pdf/single-elim-64.pdf: drabinki-pdf/single-elim-64-1.svg drabinki-pdf/single-elim-64-2.svg
	inkscape drabinki-pdf/single-elim-64-1.svg --export-pdf=drabinki-pdf/single-elim-64-1.tmp.pdf
	inkscape drabinki-pdf/single-elim-64-2.svg --export-pdf=drabinki-pdf/single-elim-64-2.tmp.pdf
	pdfunite drabinki-pdf/single-elim-64-1.tmp.pdf drabinki-pdf/single-elim-64-2.tmp.pdf drabinki-pdf/single-elim-64.pdf
drabinki-pdf/double-elim-64.pdf: drabinki-pdf/double-elim-64-1.svg drabinki-pdf/double-elim-64-2.svg drabinki-pdf/double-elim-64-3.svg
	inkscape drabinki-pdf/double-elim-64-1.svg --export-pdf=drabinki-pdf/double-elim-64-1.tmp.pdf
	inkscape drabinki-pdf/double-elim-64-2.svg --export-pdf=drabinki-pdf/double-elim-64-2.tmp.pdf
	inkscape drabinki-pdf/double-elim-64-3.svg --export-pdf=drabinki-pdf/double-elim-64-3.tmp.pdf
	pdfunite drabinki-pdf/double-elim-64-1.tmp.pdf drabinki-pdf/double-elim-64-2.tmp.pdf drabinki-pdf/double-elim-64-3.tmp.pdf drabinki-pdf/double-elim-64.pdf


cleanish:
	rm -f drabinki-pdf/*.{aux,log,svg,tmp.pdf}
	rm -f zapisy-pdf/*.{aux,log}
	rm -f handi-pdf/*.{aux,log}

clean:
	rm -f drabinki-pdf/*
	rm -f zapisy-pdf/*
	rm -f handi-pdf/*
