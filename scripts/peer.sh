#! /usr/bin/bash

# Script for running a peer
# To be run in the root of the build tree
# No jar files used
# Assumes that Peer is the main class 
#  and that it belongs to the peer package
# Modify as appropriate, so that it can be run 
#  from the root of the compiled tree

# Check number input arguments
argc=$#

if (( argc != 6 ))
then
	echo "Usage: $0 <protocol_version> <peer_ap> <IP_address> <port> <Boot_Peer_IP_address> <Boot_Peer_port>"
	exit 1
fi

# Assign input arguments to nicely named variables

ver=$1

if [[ (( $ver != '1.0' ))]]
then
  echo "Unknown Protocol Version. Known Versions: 1.0"
  exit 1
fi

id=$2
ipaddress=$3
port=$4
bpipaddress=$5
bpport=$6

# Execute the program
# Should not need to change anything but the class and its package, unless you use any jar file

# echo "java peer.Peer ${ver} ${id} ${sap} ${mc_addr} ${mc_port} ${mdb_addr} ${mdb_port} ${mdr_addr} ${mdr_port}"

java peers.Peer ${ver} ${id} ${ipaddress} ${port} ${bpipaddress} ${bpport}

