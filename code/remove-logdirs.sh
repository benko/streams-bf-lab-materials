#!/usr/bin/env bash

if [ ! "$(echo ./broker?)" = "./broker?" ]; then
    echo "ERROR: Can not find broker directories in current working dir."
    echo "       Run this script from your lab directory."
    exit 1
fi

echo "WARNING: Removing log directories for Zookeeper, all brokers, and Kafka Connect."
echo "         MAKE SURE THE PROCESSES ARE NOT RUNNING!"
echo
echo "Continue?"
select resp in "Y" "N"; do
    if [ "${resp}" = "N" ]; then
        echo "Exiting."
        exit 0
    elif [ "${resp}" = "Y" ]; then
        break
    fi
done

rm -rf broker? zookeeper connect
echo "Done."
