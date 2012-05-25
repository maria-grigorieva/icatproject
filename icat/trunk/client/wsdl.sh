#!/bin/sh

port=52742

wget --no-check-certificate https://localhost:$port/ICATService/ICAT?wsdl -O ICAT.wsdl
wget --no-check-certificate https://localhost:$port/ICATService/ICAT?xsd=1 -O ICAT.xsd

sed  s#https://localhost:$port/ICATService/ICAT?xsd=1#ICAT.xsd# ICAT.wsdl > ICAT.wsdl.new && mv ICAT.wsdl.new ICAT.wsdl
sed 's#ref="tns:\([a-z]*\)"#name="\1" type="tns:\1"#' ICAT.xsd > ICAT.xsd.new && mv ICAT.xsd.new ICAT.xsd

wget --no-check-certificate https://localhost:$port/ICATCompatService/ICATCompat?wsdl -O ICATCompat.wsdl
wget --no-check-certificate https://localhost:$port/ICATCompatService/ICATCompat?xsd=1 -O ICATCompat.xsd

sed  s#https://localhost:$port/ICATCompatService/ICATCompat?xsd=1#ICATCompat.xsd# ICATCompat.wsdl > ICATCompat.wsdl.new && mv ICATCompat.wsdl.new ICATCompat.wsdl
sed 's#ref="tns:\([a-z]*\)"#name="\1" type="tns:\1"#' ICATCompat.xsd > ICATCompat.xsd.new && mv ICATCompat.xsd.new ICATCompat.xsd
