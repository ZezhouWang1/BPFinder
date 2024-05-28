#!/bin/bash
#PBS -N BPFinder
#PBS -P vh99
#PBS -q normal
#PBS -l ncpus=16
#PBS -l mem=48GB
#PBS -o ZezhouWang.out
#PBS -e ZezhouWang.err
#PBS -l walltime=12:00:00
#PBS -m abe
#PBS -M u7439262@anu.edu.au
#PBS -J 1-10
#PBS -r y
#PBS -l wd

exec 1>ZezhouWang_${PBS_ARRAY_INDEX}.out 2>ZezhouWang_${PBS_ARRAY_INDEX}.err

START=$((PBS_ARRAY_INDEX * 200 - 199))
STOP=$((PBS_ARRAY_INDEX * 200))

echo "START=$START"
echo "STOP=$STOP"

for (( N = $START; N <= $STOP; N++ ))
do 
    LINE=$(sed -n "${N}p" Goodware.txt)
    ./BPFinders.sh $LINE
done