#!/usr/bin/env bash
MYDIR=$(dirname $0)

ERRORS=0
if [ "$(echo ${MYDIR}/core-api-producer/payload*.log)" = "${MYDIR}/core-api-producer/payload*.log" ]; then
    echo "ERROR: Missing producer log."
    ERRORS=1
fi
if [ "$(echo ${MYDIR}/core-api-consumer/payload*.log)" = "${MYDIR}/core-api-consumer/payload*.log" ]; then
    echo "ERROR: Missing consumer log."
    ERRORS=1
fi

if [ ${ERRORS} -gt 0 ]; then
    echo "WARNING: Some logs are missing, chances are that you will get inconsistent results."
    echo "         Continue?"
    select resp in "Y" "N"; do
        if [ "${resp}" = "N" ]; then
            echo "Exiting."
            exit 0
        elif [ "${resp}" = "Y" ]; then
            break
        fi
    done
fi

if [ -e "${MYDIR}/producer.log" ] || [ -e "${MYDIR}/consumer.log" ]; then
    echo "WARNING: Existing logs will be overwritten. Do you want to continue?"
    select resp in "Y" "N"; do
        if [ "${resp}" = "N" ]; then
            echo "Exiting."
            exit 0
        elif [ "${resp}" = "Y" ]; then
            break
        fi
    done
    rm -f "${MYDIR}/producer.log" "${MYDIR}/consumer.log"
fi

sort -gk2,3 -t, ${MYDIR}/core-api-producer/payload*.log > ${MYDIR}/producer.log
sort -gk2,3 -t, ${MYDIR}/core-api-consumer/payload*.log > ${MYDIR}/consumer.log

rm -fv ${MYDIR}/core-api-producer/payload*.log ${MYDIR}/core-api-consumer/payload*.log

echo "Producer and/or Consumer logs available in ${MYDIR} - old logs were removed."
