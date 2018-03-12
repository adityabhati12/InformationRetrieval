#!/usr/bin/env bash
cd crandocs
chmod +x trec_eval
./trec_eval -l3 -a c_cran.txt results.txt
