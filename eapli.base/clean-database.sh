-set e

echo "================================================="
echo "             A apagar base de dados              "
echo "================================================="

rm db/alsafe.mv.db

echo "================================================="
echo "      A dar build e a criar a base de dados      "
echo "================================================="
./build.sh