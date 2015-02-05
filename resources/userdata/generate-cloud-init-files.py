#!/bin/python
import argparse
import re

parser = argparse.ArgumentParser()
parser.add_argument('-t', '--template', type=argparse.FileType('r'), required=True)
parser.add_argument('-m', '--mill_props', type=argparse.FileType('r'), required=True)
parser.add_argument('-e', '--extended_props', type=argparse.FileType('r'), required=True)
args = parser.parse_args()

template = args.template.readlines();
#parse template file

#parse mill properties file
mill_props = args.mill_props.readlines();

#parse extended properties file
extended_props = args.extended_props.readlines();

props = {} 

#define the load props subroutine
def loadProps(props, property_file): 
	for line in property_file: 
		stripped = line.strip() 
		if not stripped == "" and not stripped.startswith("#"): 
			key, value = stripped.split('=') 
			props[key] = value 
			
	return props


#load properties into one large dictionary
props = loadProps(props, mill_props)
props = loadProps(props, extended_props)

# for each line in template
for line in template: 
	match = re.findall('\$\{([^}]+)\}', line, re.DOTALL) 
	if "MILL_CONFIG" in line: 
		print(line, end="")
		for x in mill_props:
			print(x, end="") 
		
	elif not match: 
		print(line, end="") 
	else:
		for i in match: 
			value = props[i]
			line = line.replace('${'+i+'}', value) 
			
		print(line, end="")

