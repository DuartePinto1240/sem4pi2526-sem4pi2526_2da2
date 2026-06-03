CC      = gcc
CFLAGS  = -Wall -Wextra -g
LIBS    = -lm -lpthread -lrt
TARGET  = simulation
TEST    = test_simulation
TEST102 = test_safety_violations
TEST103 = test_synchronization
TEST105 = test_us105
TEST107 = test_us107

SRC         = src/main.c src/simulation.c src/flight.c src/report.c src/json_loader.c
TEST_SRC    = tests/test_simulation.c src/simulation.c src/flight.c src/report.c src/json_loader.c
TEST102_SRC = tests/test_safety_violations.c src/simulation.c src/flight.c src/report.c src/json_loader.c
TEST103_SRC = tests/test_synchronization.c src/simulation.c src/flight.c src/report.c src/json_loader.c
TEST105_SRC = tests/test_us105.c src/simulation.c src/flight.c src/report.c src/json_loader.c
TEST107_SRC = tests/test_us107.c src/simulation.c src/flight.c src/report.c src/json_loader.c

all: $(TARGET)

$(TARGET): $(SRC)
	$(CC) $(CFLAGS) -o $(TARGET) $(SRC) $(LIBS)

test: $(TEST)
	./$(TEST)

$(TEST): $(TEST_SRC)
	$(CC) $(CFLAGS) -o $(TEST) $(TEST_SRC) $(LIBS)

test102: $(TEST102)
	./$(TEST102)

$(TEST102): $(TEST102_SRC)
	$(CC) $(CFLAGS) -o $(TEST102) $(TEST102_SRC) $(LIBS)

test103: $(TEST103)
	./$(TEST103)

$(TEST103): $(TEST103_SRC)
	$(CC) $(CFLAGS) -o $(TEST103) $(TEST103_SRC) $(LIBS)

test105: $(TEST105)
	./$(TEST105)

$(TEST105): $(TEST105_SRC)
	$(CC) $(CFLAGS) -o $(TEST105) $(TEST105_SRC) $(LIBS)

test107: $(TEST107)
	./$(TEST107)

$(TEST107): $(TEST107_SRC)
	$(CC) $(CFLAGS) -o $(TEST107) $(TEST107_SRC) $(LIBS)

test-all: $(TEST) $(TEST102) $(TEST103) $(TEST105) $(TEST107)
	./$(TEST)
	./$(TEST102)
	./$(TEST103)
	./$(TEST105)
	./$(TEST107)

clean:
	rm -f $(TARGET) $(TEST) $(TEST102) $(TEST103) $(TEST105) $(TEST107)