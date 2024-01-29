USER=kpipe
IMAGE_NAME=step-wrapper
IMAGE_TAG=$1
IMAGE=$USER/$IMAGE_NAME:$IMAGE_TAG
echo "===================================================================="
echo "  Building docker image $IMAGE"
echo "===================================================================="
docker build . -t $IMAGE
if [ $? -ne 0 ]
then
   exit 1
fi
echo "===================================================================="
echo "  Logging in to dockerhub with user $USER"
echo "===================================================================="
docker login --username $USER --password $DOCKERHUB_PUSH_TOKEN
if [ $? -ne 0 ]
then
   exit 1
fi
echo "===================================================================="
echo "  Pushing image $IMAGE_ID"
echo "===================================================================="
docker push $IMAGE
if [ $? -ne 0 ]
then
   exit 1
fi
echo "Done"