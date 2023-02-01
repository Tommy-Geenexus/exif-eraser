#!/bin/bash
set -o errexit -o nounset -o pipefail

BUNDLE="app/release/app-release.aab"
OUT="accrescent/"
OUTPUT="ExifEraser.apks"
PROPERTIES="exiferaser.properties"
TMP="tmp/"

while IFS= read -r line; do
    case $line in
    "ks="*)
        property_ks=$(echo $line | cut -d "=" -f 2);;
    "ks_pass="*)
        property_ks_pass=$(echo $line | cut -d "=" -f 2);;
    "ks_key_alias="*)
        property_ks_key_alias=$(echo $line | cut -d "=" -f 2);;
    "key_pass="*)
        property_key_pass=$(echo $line | cut -d "=" -f 2);;
    esac        
done < $PROPERTIES

rm -rf $OUT
mkdir -p $TMP && mkdir -p $OUT

echo "Downloading bundletool"
download_url=$(curl -sL https://api.github.com/repos/google/bundletool/releases/latest | grep "\"browser_download_url\":" | tr -d " \"" | cut -c 22-)
wget -q -P $TMP $download_url

echo "Building $OUTPUT"
bundletool=$TMP$(basename $download_url)
java -jar $bundletool build-apks \
    --bundle=$BUNDLE \
    --output=$OUT$OUTPUT \
    --ks=$property_ks \
    --ks-pass=pass:$property_ks_pass \
    --ks-key-alias=$property_ks_key_alias \
    --key-pass=pass:$property_key_pass

rm -rf $TMP
