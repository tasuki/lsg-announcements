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

handi: handi-pdf/handi-9.pdf handi-pdf/handi-13.pdf

handi-pdf/handi-9.pdf: handi/handi-9.tex
	xelatex -output-directory handi-pdf handi/handi-9.tex
handi-pdf/handi-13.pdf: handi/handi-13.tex
	xelatex -output-directory handi-pdf handi/handi-13.tex

cleanish:
	rm -f zapisy-pdf/*.{aux,log}

clean:
	rm -f zapisy-pdf/*
