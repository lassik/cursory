#include <sys/ioctl.h>

#include <termios.h>

#include <stdio.h>

#define FIND_LONG(name) findLong(#name, name)
#define FIND_SIZEOF(name) findSizeof(#name, sizeof(name))

static void findLong(const char *name, long value) {
    printf("public static final int %s = %ld;\n", name, value);
}

static void findSizeof(const char *name, size_t size) {
    printf("// sizeof(%s) == %zu\n", name, size);
}

int main(void) {
    FIND_LONG(NCCS);
    FIND_LONG(TCSAFLUSH);
    FIND_LONG(TIOCGWINSZ);
    FIND_SIZEOF(struct termios);
    FIND_SIZEOF(struct ttysize);
    return 0;
}
