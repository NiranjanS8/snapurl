import React from 'react'
import ShortenItem from './ShortenItem'

const ShortenUrlList = ({ data, onDelete }) => {
  return (
    <div className='grid gap-4'>
        {data.map((item) => (
            <ShortenItem key={item.id} {...item} onDelete={onDelete} />
        ))}
    </div>
  )
}

export default ShortenUrlList
