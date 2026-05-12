CC      = gcc
CFLAGS  = -Wall -Wextra -g
TARGET  = simulation
TEST    = test_simulation

SRC     = src/main.c src/simulation.c src/flight.c src/report.c
TEST_SRC = tests/test_simulation.c src/simulation.c src/flight.c src/report.c

all: $(TARGET)

$(TARGET): $(SRC)
	$(CC) $(CFLAGS) -o $(TARGET) $(SRC) -lm

test: $(TEST)
	./$(TEST)

$(TEST): $(TEST_SRC)
	$(CC) $(CFLAGS) -o $(TEST) $(TEST_SRC) -lm

clean:
	rm -f $(TARGET) $(TEST)