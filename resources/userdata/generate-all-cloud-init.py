#!/usr/bin/env python3
###################################################
# This program generates all cloud init files for the mill
# files for the six different configurations we have identified:
#       audit-worker
#       bit-worker
#       dup-worker
#       dup-producer
#       bit-producer
#       sentinel
#      
# The properties files will be merged into the final cloud init 
# file in the following order:
#       mill properties
#       node properties
#       extended properties (in the order they're listed)

# Author: Daniel Bernstein | dbernstein@duraspace.org
###################################################
import argparse
import os 
#main program execution
parser = argparse.ArgumentParser()
parser.add_argument('-m', '--mill_props', required=True, help='A mill properties file path')
parser.add_argument('-e', '--extended_props', nargs="+", required=True, help='A space separated list of one or more properties files')
parser.add_argument('-bx', '--bit_exclusions',  default=None, required=False, help='The path to an exclusion file')
parser.add_argument('-bi', '--bit_inclusions',  default=None, required=False, help='The path to an inclusion file')
parser.add_argument('-sx', '--storagestats_exclusions',  default=None, required=False, help='The path to a storage stats  exclusion file')
parser.add_argument('-si', '--storagestats_inclusions',  default=None, required=False, help='The path to a storage stats inclusion file')
parser.add_argument('-o', '--output_dir',  required=True, help='The destination directory for the generated cloud init files')
args = parser.parse_args()

extended_props_list = ', '.join(map(str, args.extended_props))
script_dir = os.path.dirname(os.path.abspath(__file__))
output_dir = args.output_dir
if not os.path.exists(os.path.dirname(output_dir)):
	os.makedirs(output_dir)

for i in ["audit-worker", "bit-worker", "bit-report-worker", "dup-worker", "dup-producer", "storage-stats-worker","sentinel"]:
        node_type = i
        output = os.path.join(output_dir, "cloud-init-%s.txt" % node_type)
        gen_script = os.path.join(script_dir, "generate-cloud-init.py")
        template  = os.path.join(script_dir, "userdata-cloud-init-multimime.txt.template")
        node_props_dir = os.path.join(script_dir, "node-props")
        node_props  = os.path.join(node_props_dir, "%s.properties" % node_type)
	
        command = "python3 %s -t %s -p %s %s %s -o %s" % (gen_script, template, args.mill_props, node_props, extended_props_list,output)
        bit_exclusions = args.bit_exclusions
        if bit_exclusions != None:
        	command = (command + " -x %s" % bit_exclusions)
        bit_inclusions = args.bit_inclusions
        if bit_inclusions != None:
        	command = (command + " -i %s" % bit_inclusions)
        
        storagestats_inclusions = args.storagestats_inclusions
        if storagestats_inclusions != None:
        	command = (command + " -si %s" % storagestats_inclusions)

        storagestats_exclusions = args.storagestats_exclusions
        if storagestats_exclusions != None:
        	command = (command + " -sx %s" % storagestats_exclusions)

        os.system(command)
