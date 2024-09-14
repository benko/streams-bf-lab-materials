#!/usr/bin/env bash
MYDIR=$(dirname $0)

echo "Removing payload logs in producer/consumer working directories..."

rm -fv ${MYDIR}/core-api-producer/payload*.log ${MYDIR}/core-api-consumer/payload*.log

echo "Done."
