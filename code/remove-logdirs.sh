#!/bin/sh

if [ ! -d "./broker0" ]; then
    echo "ERROR: Can not find broker directory in current working dir."
    echo "       Run this script from your lab directory."
    exit 1
fi

echo "WARNING: Removing log directories for Zookeeper and all brokers."
echo "         MAKE SURE THE PROCESSES ARE NOT RUNNING!"
echo
echo "Continue?"
select resp in "Y" "N"; do
    if [ "${resp}" = "N" ]; then
        echo "Exiting."
        exit 0
    fi
done

rm -rf broker{0,1,2} zookeeper
echo "Done."
