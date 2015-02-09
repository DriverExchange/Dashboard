#!/bin/bash

git log | head -1 | awk '{print $2}'
