import Modal from '@mui/material/Modal';

import React from 'react'
import CreateNewShorten from './CreateNewShorten';

const ShortenPopUp = ({ open, setOpen, refetch}) => {

    const handleClose = () => {
        setOpen(false);
    };

  return (
    <Modal
        open={open}
        onClose={handleClose}
        aria-labelledby="modal-modal-title"
        aria-describedby="modal-modal-description"
      >
        <div className='flex h-full w-full items-center justify-center bg-black/55 px-4'>
            <CreateNewShorten setOpen={setOpen} refetch={refetch} />
        </div>
      </Modal>
  )
}

export default ShortenPopUp;
