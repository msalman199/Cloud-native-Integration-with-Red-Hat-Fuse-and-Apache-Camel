#!/bin/bash

echo "=== Camel Route Monitoring ==="
echo "Timestamp: $(date)"
echo

echo "Output Directories:"
echo "- output/xml: $(ls -1 output/xml 2>/dev/null | wc -l) files"
echo "- output/json: $(ls -1 output/json 2>/dev/null | wc -l) files"
echo "- output/other: $(ls -1 output/other 2>/dev/null | wc -l) files"
echo "- transform-output: $(ls -1 transform-output 2>/dev/null | wc -l) files"
echo "- error-output/success: $(ls -1 error-output/success 2>/dev/null | wc -l) files"
echo "- error-output/failed: $(ls -1 error-output/failed 2>/dev/null | wc -l) files"
