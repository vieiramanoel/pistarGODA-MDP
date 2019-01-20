chmod +x eval_formula.sh

basetime=$(date +%s%N)
./eval_formula.sh reliability.out
echo "Reliability runtime: $(echo "scale=9;($(date +%s%N) - ${basetime})/(1*10^09)" | bc) seconds"
printf "\n"

basetime=$(date +%s%N)
./eval_formula.sh cost.out
echo "Cost runtime: $(echo "scale=9;($(date +%s%N) - ${basetime})/(1*10^09)" | bc) seconds"
printf "\n"