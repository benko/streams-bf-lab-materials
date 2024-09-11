#!/bin/sh
MYDIR=$(dirname $0)

if [ ! -e "${MYDIR}/core-api-producer/payload.log" ]; then
    echo "ERROR: Missing producer log."
    exit 1
fi
if [ "$(echo ${MYDIR}/core-api-consumer/payload*.log)" = "${MYDIR}/core-api-consumer/payload*.log" ]; then
    echo "ERROR: Missing consumer log."
    exit 1
fi

if [ -e "${MYDIR}/producer.log" ] || [ -e "${MYDIR}/consumerl.log" ]; then
    echo "WARNING: Existing logs will be overwritten. Do you want to continue?"
    select resp in "Y" "N"; do
        if [ "${resp}" = "N" ]; then
            echo "Exiting."
            exit 0
        fi
    done
    rm -f "${MYDIR}/producer.log" "${MYDIR}/consumer.log"
fi

sort -gk2,3 -t, ${MYDIR}/core-api-producer/payload.log > ${MYDIR}/producer.log
sort -gk2,3 -t, ${MYDIR}/core-api-consumer/payload*.log > ${MYDIR}/consumer.log
rm -f ${MYDIR}/core-api-producer/payload.log ${MYDIR}/core-api-consumer/payload*.log
echo "Producer and Consumer logs available in ${MYDIR} - old logs removed."
