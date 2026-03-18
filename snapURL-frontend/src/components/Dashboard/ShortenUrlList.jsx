import React from 'react'
import ShortenItem from './ShortenItem'

const ShortenUrlList = ({ data }) => {
  return (
    <div className='grid gap-4'>
        {data.map((item) => (
            <ShortenItem key={item.id} {...item} />
        ))}
    </div>
  )
}

export default ShortenUrlList
