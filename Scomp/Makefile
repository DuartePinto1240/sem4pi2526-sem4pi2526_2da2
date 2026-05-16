CC      = gcc
CFLAGS  = -Wall -Wextra -g
TARGET  = simulation
TEST    = test_simulation
TEST102 = test_safety_violations

SRC          = src/main.c src/simulation.c src/flight.c src/report.c
TEST_SRC     = tests/test_simulation.c src/simulation.c src/flight.c src/report.c
TEST102_SRC  = tests/test_safety_violations.c src/simulation.c src/flight.c src/report.c

all: $(TARGET)

$(TARGET): $(SRC)
	$(CC) $(CFLAGS) -o $(TARGET) $(SRC) -lm

test: $(TEST)
	./$(TEST)

$(TEST): $(TEST_SRC)
	$(CC) $(CFLAGS) -o $(TEST) $(TEST_SRC) -lm

test102: $(TEST102)
	./$(TEST102)

$(TEST102): $(TEST102_SRC)
	$(CC) $(CFLAGS) -o $(TEST102) $(TEST102_SRC) -lm

test-all: $(TEST) $(TEST102)
	./$(TEST)
	./$(TEST102)

clean:
	rm -f $(TARGET) $(TEST) $(TEST102)