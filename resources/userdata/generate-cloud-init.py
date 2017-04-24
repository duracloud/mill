#!/usr/bin/env python3
###################################################
# This program generates a single cloud init file for the mill
# based on a template, a list of properties, and in/exclusion 
# files.  All the properties will be merged in the order they are 
# specified on the command line and then used to replace any key references
# in the template couched in ${key} format. All properties will be inserted below
# MILL_CONFIG if it is defined.  The exclusion and inclusion files will be inserted
# at BIT_EXCLUSIONS and BIT_INCLUSIONS respectively.

# Author: Daniel Bernstein | dbernstein@duraspace.org
###################################################
import argparse
import re
import os
import collections
#define the load_props subroutine
def load_props(props, property_file): 
	for line in property_file: 
		stripped = line.strip() 
		if not stripped == "" and not stripped.startswith("#"): 
			key, value = stripped.split('=') 
			props[key] = value 
        		
	return props


#main program execution
parser = argparse.ArgumentParser()
parser.add_argument('-t', '--template', type=argparse.FileType('r'), required=True)
parser.add_argument('-p', '--property-files', nargs="+", type=argparse.FileType('r'), required=True)
parser.add_argument('-x', '--bit_exclusions',  type=argparse.FileType('r'), default=None, required=False)
parser.add_argument('-i', '--bit_inclusions',  type=argparse.FileType('r'), default=None, required=False)
parser.add_argument('-sx', '--storagestats_exclusions',  type=argparse.FileType('r'), default=None, required=False)
parser.add_argument('-si', '--storagestats_inclusions',  type=argparse.FileType('r'), default=None, required=False)
parser.add_argument('-o', '--output_file',  required=True)
args = parser.parse_args()

output_file = args.output_file

if not os.path.exists(os.path.dirname(output_file)):
	os.makedirs(os.path.dirname(output_file))
       
output = open(output_file, "w+");
	  
template = args.template.readlines();
bit_inclusions = args.bit_inclusions
bit_exclusions = args.bit_exclusions
storagestats_inclusions = args.storagestats_inclusions
storagestats_exclusions = args.storagestats_exclusions


props = {}

for f in args.property_files: 
	props = load_props(props,f.readlines())

#sort the properties
props = collections.OrderedDict(sorted(props.items()))

# for each line in template
for line in template: 
	match = re.findall('\$\{([^}]+)\}', line, re.DOTALL) 
	if "MILL_CONFIG" in line: 
		output.write(line)
		for x in props:
			output.write(x+"="+props[x]+"\n")
		
	elif "BIT_INCLUSIONS" in line and bit_inclusions != None: 
		output.write(line)
		for x in bit_inclusions.readlines():
			output.write(x) 
		
	elif "BIT_EXCLUSIONS" in line and bit_exclusions != None: 
		output.write(line)
		for x in bit_exclusions.readlines():
			output.write(x) 
 
	elif "STORAGE_STATS_INCLUSIONS" in line and storagestats_inclusions != None:
		output.write(line)
		for x in storagestats_inclusions.readlines():
			output.write(x) 
		
	elif "STORAGE_STATS_EXCLUSIONS" in line and storagestats_exclusions != None:
		output.write(line)
		for x in storagestats_exclusions.readlines():
			output.write(x) 

	elif not match: 
		output.write(line) 
	else:
		for i in match: 
			value = props[i]
			line = line.replace('${'+i+'}', value) 
			
		output.write(line)


output.close()
