#!/bin/python
import argparse
import re

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
parser.add_argument('-m', '--mill_props', type=argparse.FileType('r'), required=True)
parser.add_argument('-e', '--extended_props', nargs="+", type=argparse.FileType('r'), required=True)
args = parser.parse_args()

template = args.template.readlines();
#parse template file

#parse mill properties file
mill_props = args.mill_props.readlines();

#parse extended properties file
extended_props = {}

for f in args.extended_props: 
	extended_props = load_props(extended_props,f.readlines())

props = {} 

#load properties into one large dictionary
props = load_props(props, mill_props)
#overlay extended props on props
props.update(extended_props)

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

