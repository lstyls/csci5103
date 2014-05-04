# GNU Makefile for building user programs to run on top of Nachos
#
# NOTE:
#    The value of the ARCHDIR environment variable must be set before using
#    this makefile.

ASFLAGS = -mips1
CPPFLAGS =
CFLAGS = -O2 -B$(ARCHDIR) -G 0 -Wa,-mips1 -nostdlib -ffreestanding
LDFLAGS = -s -T script -N -warn-common -warn-constructors -warn-multiple-gp

# Point to the MIPS-specific executables
CC = $(ARCHDIR)/mips-gcc
AS = $(ARCHDIR)/mips-as
LD = $(ARCHDIR)/mips-ld
CPP = $(ARCHDIR)/mips-cpp
AR = $(ARCHDIR)/mips-ar
RANLIB = $(ARCHDIR)/mips-ranlib

STDLIB_H = stdio.h stdlib.h ag.h
STDLIB_C = stdio.c stdlib.c
STDLIB_O = start.o stdio.o stdlib.o

LIB = assert atoi printf readline stdio strncmp strcat strcmp strcpy strlen memcpy memset
NLIB = libnachos.a

TARGETS = safematmult halt sh matmult sort echo cat cp mv rm type #chat chatserver

.SECONDARY: $(patsubst %.c,%.o,$(wildcard *.c))

all: $(patsubst %,%.coff,$(TARGETS))

ag: grade-file.coff grade-exec.coff grade-mini.coff grade-dumb.coff

clean:
	rm -f strt.s *.o *.coff $(NLIB)

agclean: clean
	rm -f f1-* f2-*

$(NLIB): $(patsubst %,$(NLIB)(%.o),$(LIB)) start.o
	$(RANLIB) $(NLIB)

start.o: start.s syscall.h
	$(CPP) $(CPPFLAGS) start.s > strt.s
	$(AS) $(ASFLAGS) -o start.o strt.s
	rm strt.s

%.o: %.c *.h
	$(CC) $(CFLAGS) -c $<

%.coff: %.o $(NLIB)
	$(LD) $(LDFLAGS) -o $@ $< start.o -lnachos
